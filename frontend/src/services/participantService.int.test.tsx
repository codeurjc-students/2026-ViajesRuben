// @vitest-environment node
import { afterEach, describe, expect, test, vi } from 'vitest';
import { newUser, png, sendCookie, sessionCookie } from '../integrationHelpers';
import { createInvitation, respondToInvitation } from './invitationService';
import { getParticipants, removeParticipant } from './participantService';
import { createTrip, getTrip, getTrips } from './tripService';
import { uploadAvatar } from './userService';

const tripData = {
  name: 'Roma',
  destination: 'Roma',
  startDate: '2027-07-10',
  endDate: '2027-07-14',
  budget: null,
  description: null,
};

afterEach(() => {
  vi.restoreAllMocks();
});

/** Starts sending the session of the user instead of the previous one. */
function actAs(user: { cookie: string }) {
  vi.restoreAllMocks();
  sendCookie(user.cookie);
}

/** Ana creates a trip, invites a new user and that user accepts; the session left is Ana's. */
async function tripWithOneParticipant(name = 'Bob') {
  const ana = await newUser('Ana');
  const trip = await createTrip(tripData);
  const bob = await newUser(name);
  actAs(ana);
  const invitation = await createInvitation(trip.id, bob.email);
  actAs(bob);
  await respondToInvitation(invitation.id, 'ACCEPTED');
  actAs(ana);
  return { ana, bob, trip };
}

describe('I3 participantService against the REST API', () => {
  test('without a session every operation is a 401', async () => {
    await expect(getParticipants(1)).rejects.toMatchObject({ status: 401 });
    await expect(removeParticipant(1, 2)).rejects.toMatchObject({ status: 401 });
  });

  test('the creator lists who joined, with emails and without themselves', async () => {
    const { bob, trip } = await tripWithOneParticipant();

    const page = await getParticipants(trip.id);

    expect(page).toMatchObject({ page: 0, size: 10, totalElements: 1, last: true });
    expect(page.content[0]).toMatchObject({
      userId: bob.user.id,
      name: 'Bob',
      email: bob.email,
      avatarId: null,
    });
    expect(page.content[0].joinedAt).toEqual(expect.any(String));
    expect(Object.keys(page.content[0]).sort((a, b) => a.localeCompare(b))).toEqual([
      'avatarId',
      'email',
      'joinedAt',
      'name',
      'userId',
    ]);
  });

  test('a participant sees who joined but never an email', async () => {
    const { bob, trip } = await tripWithOneParticipant();
    actAs(bob);

    const page = await getParticipants(trip.id);

    expect(page.content).toHaveLength(1);
    expect(page.content[0]).toMatchObject({ userId: bob.user.id, name: 'Bob', email: null });
  });

  test('the avatar id of a participant is the UUID of the uploaded picture', async () => {
    const { ana, bob, trip } = await tripWithOneParticipant();
    actAs(bob);
    const uploaded = await uploadAvatar(bob.user.id, png());
    actAs(ana);

    const page = await getParticipants(trip.id);

    expect(page.content[0].avatarId).toBe(uploaded.id);
    expect(page.content[0].avatarId).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/,
    );
  });

  test('someone who is not in the trip cannot list it: 403', async () => {
    const { trip } = await tripWithOneParticipant();
    vi.restoreAllMocks();
    await newUser('Carla');

    await expect(getParticipants(trip.id)).rejects.toMatchObject({ status: 403 });
  });

  test('a trip that does not exist is a 404', async () => {
    await newUser();

    await expect(getParticipants(999_999_999)).rejects.toMatchObject({ status: 404 });
    await expect(removeParticipant(999_999_999, 1)).rejects.toMatchObject({ status: 404 });
  });

  test('the creator removes a participant, who then gets a 403 and loses the trip from their list', async () => {
    const { ana, bob, trip } = await tripWithOneParticipant();
    actAs(bob);
    await expect(getTrip(trip.id)).resolves.toMatchObject({ id: trip.id });
    actAs(ana);

    await expect(removeParticipant(trip.id, bob.user.id)).resolves.toBeUndefined();

    expect((await getParticipants(trip.id)).totalElements).toBe(0);
    actAs(bob);
    await expect(getTrip(trip.id)).rejects.toMatchObject({ status: 403 });
    expect((await getTrips()).content).toEqual([]);
  });

  test('a participant leaves the trip by removing themselves', async () => {
    const { bob, trip } = await tripWithOneParticipant();
    actAs(bob);

    await expect(removeParticipant(trip.id, bob.user.id)).resolves.toBeUndefined();

    await expect(getTrip(trip.id)).rejects.toMatchObject({ status: 403 });
    await expect(removeParticipant(trip.id, bob.user.id)).rejects.toMatchObject({ status: 403 });
  });

  test('a participant cannot remove another one or the creator: 403', async () => {
    const { ana, bob, trip } = await tripWithOneParticipant();
    actAs(ana);
    const carla = await newUser('Carla');
    actAs(ana);
    const invitation = await createInvitation(trip.id, carla.email);
    actAs(carla);
    await respondToInvitation(invitation.id, 'ACCEPTED');
    actAs(bob);

    await expect(removeParticipant(trip.id, carla.user.id)).rejects.toMatchObject({ status: 403 });
    await expect(removeParticipant(trip.id, ana.user.id)).rejects.toMatchObject({ status: 403 });
    actAs(ana);
    expect((await getParticipants(trip.id)).totalElements).toBe(2);
  });

  test('the creator cannot leave their own trip: 409', async () => {
    const { ana, trip } = await tripWithOneParticipant();

    await expect(removeParticipant(trip.id, ana.user.id)).rejects.toMatchObject({ status: 409 });
    await expect(getTrip(trip.id)).resolves.toMatchObject({ id: trip.id });
  });

  test('the administrator cannot remove participants: 403', async () => {
    const { bob, trip } = await tripWithOneParticipant();
    vi.restoreAllMocks();
    // The development administrator (ADMIN_EMAIL / ADMIN_PASSWORD_HASH defaults), as in CI.
    sendCookie(await sessionCookie('admin@tripmate.com', 'admin'));

    await expect(removeParticipant(trip.id, bob.user.id)).rejects.toMatchObject({ status: 403 });
    expect((await getParticipants(trip.id)).totalElements).toBe(1);
  });

  test('removing someone who is not a participant is a 404', async () => {
    const { ana, trip } = await tripWithOneParticipant();
    actAs(ana);

    await expect(removeParticipant(trip.id, 999_999_999)).rejects.toMatchObject({ status: 404 });
  });

  test('the trip list and the trip report how many people joined and follow every change', async () => {
    const { ana, bob, trip } = await tripWithOneParticipant();

    expect((await getTrips()).content[0].participantCount).toBe(1);
    expect((await getTrip(trip.id)).participantCount).toBe(1);
    actAs(bob);
    expect((await getTrips()).content[0].participantCount).toBe(1);
    await removeParticipant(trip.id, bob.user.id);
    actAs(ana);

    expect((await getTrips()).content[0].participantCount).toBe(0);
    expect((await getTrip(trip.id)).participantCount).toBe(0);
  });

  test('a removed user can be invited again and joins again', async () => {
    const { ana, bob, trip } = await tripWithOneParticipant();
    await removeParticipant(trip.id, bob.user.id);
    const invitation = await createInvitation(trip.id, bob.email);
    actAs(bob);

    await respondToInvitation(invitation.id, 'ACCEPTED');

    await expect(getTrip(trip.id)).resolves.toMatchObject({ id: trip.id });
    actAs(ana);
    expect((await getParticipants(trip.id)).totalElements).toBe(1);
  });
});
