// @vitest-environment node
import { afterEach, describe, expect, test, vi } from 'vitest';
import { newUser } from '../integrationHelpers';
import type { ActivityData } from '../types';
import { createActivity, updateActivity } from './activityService';
import { getConflicts } from './conflictService';
import { createTrip, updateTrip } from './tripService';

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

describe('B6 conflictService against the REST API', () => {
  test('without a session it is a 401', async () => {
    await expect(getConflicts(1)).rejects.toMatchObject({ status: 401 });
  });

  test('a trip without activities has no conflicts', async () => {
    await newUser();
    const trip = await createTrip({ ...tripData, budget: 100 });

    await expect(getConflicts(trip.id)).resolves.toEqual({
      overlaps: [],
      outsideDates: [],
      budget: null,
    });
  });

  test('overlapping activities are reported and touching ones are not', async () => {
    await newUser();
    const trip = await createTrip(tripData);
    const tour = await createActivity(
      trip.id,
      activityData({ name: 'Visita', startTime: '10:00', endTime: '12:00' }),
    );
    const lunch = await createActivity(
      trip.id,
      activityData({ name: 'Comida', startTime: '11:00', endTime: '13:00' }),
    );
    await createActivity(
      trip.id,
      activityData({ name: 'Siesta', startTime: '13:00', endTime: '14:00' }),
    );

    const { overlaps } = await getConflicts(trip.id);

    expect(overlaps).toEqual([
      {
        date: '2027-07-11',
        firstId: tour.id,
        firstName: 'Visita',
        firstStart: '10:00:00',
        firstEnd: '12:00:00',
        secondId: lunch.id,
        secondName: 'Comida',
        secondStart: '11:00:00',
        secondEnd: '13:00:00',
      },
    ]);
  });

  test('editing one of the overlapping activities clears the conflict', async () => {
    await newUser();
    const trip = await createTrip(tripData);
    await createActivity(trip.id, activityData({ startTime: '10:00', endTime: '12:00' }));
    const lunch = await createActivity(
      trip.id,
      activityData({ name: 'Comida', startTime: '11:00', endTime: '13:00' }),
    );
    expect((await getConflicts(trip.id)).overlaps).toHaveLength(1);

    await updateActivity(
      trip.id,
      lunch.id,
      activityData({ name: 'Comida', startTime: '12:00', endTime: '13:00' }),
    );

    expect((await getConflicts(trip.id)).overlaps).toEqual([]);
  });

  test('shortening the trip is accepted and reports the activities left outside', async () => {
    await newUser();
    const trip = await createTrip(tripData);
    await createActivity(trip.id, activityData({ date: '2027-07-10' }));
    const last = await createActivity(trip.id, activityData({ name: 'Playa', date: '2027-07-14' }));

    await updateTrip(trip.id, { ...tripData, endDate: '2027-07-12' });

    expect((await getConflicts(trip.id)).outsideDates).toEqual([
      { activityId: last.id, name: 'Playa', date: '2027-07-14' },
    ]);
  });

  test('a total above the budget is reported and equal to it is not', async () => {
    await newUser();
    const trip = await createTrip({ ...tripData, budget: 100 });
    await createActivity(trip.id, activityData({ cost: 60 }));
    await createActivity(trip.id, activityData({ name: 'Cena', cost: 40 }));
    expect((await getConflicts(trip.id)).budget).toBeNull();

    await createActivity(trip.id, activityData({ name: 'Recuerdos', cost: 0.5 }));

    expect((await getConflicts(trip.id)).budget).toEqual({ totalCost: 100.5, budget: 100 });
  });

  test('a trip without budget never has a budget conflict', async () => {
    await newUser();
    const trip = await createTrip(tripData);
    await createActivity(trip.id, activityData({ cost: 99999 }));

    expect((await getConflicts(trip.id)).budget).toBeNull();
  });

  test('another user gets a 403 and an unknown trip a 404', async () => {
    await newUser('Ana');
    const trip = await createTrip(tripData);
    vi.restoreAllMocks();
    await newUser('Bob');

    await expect(getConflicts(trip.id)).rejects.toMatchObject({ status: 403 });
    await expect(getConflicts(999999999)).rejects.toMatchObject({ status: 404 });
  });
});
