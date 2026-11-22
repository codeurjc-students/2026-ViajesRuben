// @vitest-environment node
import { afterEach, describe, expect, test, vi } from 'vitest';
import { newUser, sendCookie, sessionCookie } from '../integrationHelpers';
import {
  createInvitation,
  getInvitationByToken,
  getInvitations,
  getMyInvitations,
  respondToInvitation,
} from './invitationService';
import { createTrip, deleteTrip, getTrip, getTrips } from './tripService';

const tripData = {
  name: 'Roma',
  destination: 'Roma',
  startDate: '2027-07-10',
  endDate: '2027-07-14',
  budget: null,
  description: null,
};

function friendEmail() {
  return `friend-${Date.now()}-${Math.floor(Math.random() * 1e6)}@example.com`;
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe('I1 invitationService against the REST API', () => {
  test('without a session every invitation operation is a 401', async () => {
    await expect(getInvitations(1)).rejects.toMatchObject({ status: 401 });
    await expect(createInvitation(1, 'friend@example.com')).rejects.toMatchObject({
      status: 401,
    });
  });

  test('invites an email, normalised and without the token, and lists it as pending', async () => {
    await newUser();
    const trip = await createTrip(tripData);
    const email = friendEmail();

    const created = await createInvitation(trip.id, email.toUpperCase());

    expect(created).toMatchObject({ invitedEmail: email, status: 'PENDING' });
    expect(Object.keys(created).sort((a, b) => a.localeCompare(b))).toEqual([
      'id',
      'invitedEmail',
      'respondedAt',
      'sentAt',
      'status',
    ]);
    const page = await getInvitations(trip.id);
    expect(page).toMatchObject({ page: 0, size: 10, totalElements: 1, last: true });
    expect(page.content).toEqual([created]);
  });

  test('lists the invitations newest first, ten at a time', async () => {
    await newUser();
    const trip = await createTrip(tripData);
    const emails: string[] = [];
    for (let i = 0; i < 11; i++) {
      emails.push(friendEmail());
      await createInvitation(trip.id, emails[i]);
    }

    const first = await getInvitations(trip.id);
    const second = await getInvitations(trip.id, 1);

    expect(first.content).toHaveLength(10);
    expect(first.last).toBe(false);
    expect(first.content[0].invitedEmail).toBe(emails[10]);
    expect(second.content.map((invitation) => invitation.invitedEmail)).toEqual([emails[0]]);
    expect(second.last).toBe(true);
  });

  test('a second pending invitation for the same email is a 409', async () => {
    await newUser();
    const trip = await createTrip(tripData);
    const email = friendEmail();
    await createInvitation(trip.id, email);

    await expect(createInvitation(trip.id, email)).rejects.toMatchObject({ status: 409 });
    expect((await getInvitations(trip.id)).totalElements).toBe(1);
  });

  test("the creator's own email and an invalid email are a 400", async () => {
    const { email } = await newUser();
    const trip = await createTrip(tripData);

    await expect(createInvitation(trip.id, email)).rejects.toMatchObject({ status: 400 });
    await expect(createInvitation(trip.id, 'not-an-email')).rejects.toMatchObject({
      status: 400,
    });
    expect((await getInvitations(trip.id)).totalElements).toBe(0);
  });

  test('another user cannot invite to or list the invitations of the trip: 403', async () => {
    await newUser();
    const trip = await createTrip(tripData);
    await createInvitation(trip.id, friendEmail());

    vi.restoreAllMocks();
    await newUser('Bob');

    await expect(createInvitation(trip.id, friendEmail())).rejects.toMatchObject({ status: 403 });
    await expect(getInvitations(trip.id)).rejects.toMatchObject({ status: 403 });
  });

  test('the administrator cannot invite to or list the invitations of the trip: 403', async () => {
    await newUser();
    const trip = await createTrip(tripData);

    vi.restoreAllMocks();
    // The development administrator (ADMIN_EMAIL / ADMIN_PASSWORD_HASH defaults), as in CI.
    sendCookie(await sessionCookie('admin@tripmate.com', 'admin'));

    await expect(createInvitation(trip.id, friendEmail())).rejects.toMatchObject({ status: 403 });
    await expect(getInvitations(trip.id)).rejects.toMatchObject({ status: 403 });
  });

  test('a trip that does not exist is a 404', async () => {
    await newUser();

    await expect(getInvitations(999_999_999)).rejects.toMatchObject({ status: 404 });
    await expect(createInvitation(999_999_999, friendEmail())).rejects.toMatchObject({
      status: 404,
    });
  });

  test('deleting the trip deletes its invitations', async () => {
    await newUser();
    const trip = await createTrip(tripData);
    await createInvitation(trip.id, friendEmail());

    await deleteTrip(trip.id);

    await expect(getInvitations(trip.id)).rejects.toMatchObject({ status: 404 });
  });
});

/** Ana creates a trip and invites Bob, a registered user; the session left is Ana's. */
async function inviteSomeone() {
  const ana = await newUser('Ana');
  const trip = await createTrip(tripData);
  const bob = await newUser('Bob');
  actAs(ana);
  const invitation = await createInvitation(trip.id, bob.email);
  return { ana, bob, trip, invitation };
}

/** Starts sending the session of the user instead of the previous one. */
function actAs(user: { cookie: string }) {
  vi.restoreAllMocks();
  sendCookie(user.cookie);
}

describe('I2 invitationService against the REST API', () => {
  test('without a session every received-invitation operation is a 401', async () => {
    await expect(getMyInvitations()).rejects.toMatchObject({ status: 401 });
    await expect(getInvitationByToken('x')).rejects.toMatchObject({ status: 401 });
    await expect(respondToInvitation(1, 'ACCEPTED')).rejects.toMatchObject({ status: 401 });
  });

  test('the invited user sees the pending invitation with the trip and the inviter', async () => {
    const { bob, trip } = await inviteSomeone();
    actAs(bob);

    const page = await getMyInvitations();

    expect(page).toMatchObject({ page: 0, size: 10, totalElements: 1, last: true });
    expect(page.content[0]).toMatchObject({
      tripId: trip.id,
      tripName: 'Roma',
      inviterName: 'Ana',
      status: 'PENDING',
      respondedAt: null,
    });
    expect(Object.keys(page.content[0]).sort((a, b) => a.localeCompare(b))).toEqual([
      'id',
      'inviterName',
      'respondedAt',
      'sentAt',
      'status',
      'tripId',
      'tripName',
    ]);
  });

  test('accepting makes the trip readable and listed, and shows the creator the new status', async () => {
    const { ana, bob, trip, invitation } = await inviteSomeone();
    actAs(bob);
    await expect(getTrip(trip.id)).rejects.toMatchObject({ status: 403 });

    const answered = await respondToInvitation(invitation.id, 'ACCEPTED');

    expect(answered).toMatchObject({ id: invitation.id, status: 'ACCEPTED' });
    expect(answered.respondedAt).not.toBeNull();
    expect((await getTrip(trip.id)).name).toBe('Roma');
    expect((await getTrips()).content.map((listed) => listed.id)).toEqual([trip.id]);
    expect((await getMyInvitations()).totalElements).toBe(0);
    actAs(ana);
    expect((await getInvitations(trip.id)).content[0]).toMatchObject({ status: 'ACCEPTED' });
  });

  test('rejecting shares nothing and the invitation leaves the list', async () => {
    const { bob, trip, invitation } = await inviteSomeone();
    actAs(bob);

    await expect(respondToInvitation(invitation.id, 'REJECTED')).resolves.toMatchObject({
      status: 'REJECTED',
    });

    expect((await getMyInvitations()).totalElements).toBe(0);
    await expect(getTrip(trip.id)).rejects.toMatchObject({ status: 403 });
  });

  test('answering twice is a 409', async () => {
    const { bob, invitation } = await inviteSomeone();
    actAs(bob);
    await respondToInvitation(invitation.id, 'REJECTED');

    await expect(respondToInvitation(invitation.id, 'ACCEPTED')).rejects.toMatchObject({
      status: 409,
    });
  });

  test('another user cannot answer the invitation: 403', async () => {
    const { invitation } = await inviteSomeone();
    vi.restoreAllMocks();
    await newUser('Carla');

    await expect(respondToInvitation(invitation.id, 'ACCEPTED')).rejects.toMatchObject({
      status: 403,
    });
    expect((await getMyInvitations()).totalElements).toBe(0);
  });

  test('inviting someone who already participates is a 409', async () => {
    const { ana, bob, trip, invitation } = await inviteSomeone();
    actAs(bob);
    await respondToInvitation(invitation.id, 'ACCEPTED');
    actAs(ana);

    await expect(createInvitation(trip.id, bob.email)).rejects.toMatchObject({ status: 409 });
  });

  test('an unknown invitation or token is a 404', async () => {
    await newUser();

    await expect(respondToInvitation(999_999_999, 'ACCEPTED')).rejects.toMatchObject({
      status: 404,
    });
    await expect(getInvitationByToken('does-not-exist')).rejects.toMatchObject({ status: 404 });
  });

  test('deleting the trip deletes the pending invitation: 404 for the invited user', async () => {
    const { ana, bob, trip, invitation } = await inviteSomeone();
    actAs(ana);
    await deleteTrip(trip.id);
    actAs(bob);

    await expect(respondToInvitation(invitation.id, 'ACCEPTED')).rejects.toMatchObject({
      status: 404,
    });
    expect((await getMyInvitations()).totalElements).toBe(0);
  });
});
