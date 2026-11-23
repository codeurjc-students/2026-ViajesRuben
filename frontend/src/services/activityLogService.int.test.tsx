// @vitest-environment node
import { afterEach, describe, expect, test, vi } from 'vitest';
import { newUser, sendCookie } from '../integrationHelpers';
import type { ActivityData } from '../types';
import { getActivityLog } from './activityLogService';
import { createActivity, deleteActivity, updateActivity } from './activityService';
import { createInvitation, respondToInvitation } from './invitationService';
import { createTrip, deleteTrip } from './tripService';

const tripData = {
  name: 'Roma',
  destination: 'Roma',
  startDate: '2027-07-10',
  endDate: '2027-07-14',
  budget: null,
  description: null,
};

function activityData(overrides: Partial<ActivityData> = {}): ActivityData {
  return {
    name: 'Coliseo',
    date: '2027-07-11',
    startTime: null,
    endTime: null,
    location: null,
    description: null,
    cost: null,
    category: null,
    ...overrides,
  };
}

afterEach(() => {
  vi.restoreAllMocks();
});

/** Starts sending the session of the user instead of the previous one. */
function actAs(user: { cookie: string }) {
  vi.restoreAllMocks();
  sendCookie(user.cookie);
}

/** Ana creates a trip, invites a new user and that user accepts; the session left is Ana's. */
async function tripWithOneParticipant() {
  const ana = await newUser('Ana');
  const trip = await createTrip(tripData);
  const bob = await newUser('Bob');
  actAs(ana);
  const invitation = await createInvitation(trip.id, bob.email);
  actAs(bob);
  await respondToInvitation(invitation.id, 'ACCEPTED');
  actAs(ana);
  return { ana, bob, trip };
}

describe('I4 activityLogService against the REST API', () => {
  test('without a session it is a 401', async () => {
    await expect(getActivityLog(1)).rejects.toMatchObject({ status: 401 });
  });

  test('a new trip has an empty log', async () => {
    await newUser('Ana');
    const trip = await createTrip(tripData);

    await expect(getActivityLog(trip.id)).resolves.toMatchObject({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      last: true,
    });
  });

  test('every change appears once, newest first, with who did it and what changed', async () => {
    const { ana, bob, trip } = await tripWithOneParticipant();
    const museum = await createActivity(trip.id, activityData({ name: 'Museo' }));
    actAs(bob);
    await updateActivity(trip.id, museum.id, activityData({ name: 'Louvre', cost: 20 }));
    await deleteActivity(trip.id, museum.id);
    actAs(ana);

    const page = await getActivityLog(trip.id);

    expect(page.totalElements).toBe(3);
    expect(page.content.map((entry) => [entry.action, entry.userName, entry.activityName])).toEqual(
      [
        ['DELETED', 'Bob', 'Louvre'],
        ['UPDATED', 'Bob', 'Louvre'],
        ['CREATED', 'Ana', 'Museo'],
      ],
    );
    expect(page.content[1].changedFields).toEqual(['name', 'cost']);
    expect(page.content[0].changedFields).toEqual([]);
    expect(page.content.map((entry) => entry.userId)).toEqual([
      bob.user.id,
      bob.user.id,
      ana.user.id,
    ]);
    // The activity is gone, so no line points at it any more; the names are kept.
    expect(page.content.map((entry) => entry.activityId)).toEqual([null, null, null]);
    expect(page.content[0].createdAt).toEqual(expect.any(String));
    expect(Object.keys(page.content[0]).sort((a, b) => a.localeCompare(b))).toEqual([
      'action',
      'activityId',
      'activityName',
      'changedFields',
      'createdAt',
      'id',
      'userId',
      'userName',
    ]);
  });

  test('a line keeps the id of an activity that still exists', async () => {
    await newUser('Ana');
    const trip = await createTrip(tripData);
    const activity = await createActivity(trip.id, activityData());

    const page = await getActivityLog(trip.id);

    expect(page.content[0]).toMatchObject({ activityId: activity.id, action: 'CREATED' });
  });

  test('is paged: the page and the size are the ones asked for', async () => {
    await newUser('Ana');
    const trip = await createTrip(tripData);
    for (const name of ['Uno', 'Dos', 'Tres']) {
      await createActivity(trip.id, activityData({ name }));
    }

    const first = await getActivityLog(trip.id, 0, 2);
    const second = await getActivityLog(trip.id, 1, 2);

    expect(first).toMatchObject({ size: 2, totalElements: 3, last: false });
    expect(first.content.map((entry) => entry.activityName)).toEqual(['Tres', 'Dos']);
    expect(second.content.map((entry) => entry.activityName)).toEqual(['Uno']);
    expect(second.last).toBe(true);
  });

  test('a participant reads it too, and someone outside the trip gets 403', async () => {
    const { bob, trip } = await tripWithOneParticipant();
    await createActivity(trip.id, activityData());
    actAs(bob);

    await expect(getActivityLog(trip.id)).resolves.toMatchObject({ totalElements: 1 });
    vi.restoreAllMocks();
    await newUser('Carla');

    await expect(getActivityLog(trip.id)).rejects.toMatchObject({ status: 403 });
  });

  test('a trip that does not exist is a 404', async () => {
    await newUser('Ana');

    await expect(getActivityLog(999_999_999)).rejects.toMatchObject({ status: 404 });
  });

  test('deleting the trip deletes its log', async () => {
    await newUser('Ana');
    const trip = await createTrip(tripData);
    await createActivity(trip.id, activityData());

    await deleteTrip(trip.id);

    await expect(getActivityLog(trip.id)).rejects.toMatchObject({ status: 404 });
  });
});
