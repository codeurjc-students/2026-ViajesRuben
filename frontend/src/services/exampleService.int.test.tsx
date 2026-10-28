// @vitest-environment node
import { afterEach, describe, expect, test, vi } from 'vitest';
import { newUser } from '../integrationHelpers';
import { getExampleActivities, getExampleTrip, getExampleTrips } from './exampleService';
import { createTrip } from './tripService';

afterEach(() => {
  vi.restoreAllMocks();
});

// The example trips are the administrator's, loaded with the sample data (SAMPLE_DATA, on by
// default): four trips with an itinerary. Nobody can create them without the administrator.
describe('B1 exampleService against the REST API', () => {
  test('an anonymous visitor lists the example trips three at a time, with a preview of the first days', async () => {
    const first = await getExampleTrips(0, 3);
    const second = await getExampleTrips(1, 3);

    expect(first).toMatchObject({ page: 0, size: 3, last: false });
    expect(first.totalElements).toBeGreaterThanOrEqual(4);
    expect(first.content).toHaveLength(3);
    expect(second.content.length).toBeGreaterThanOrEqual(1);
    expect(second.content[0].trip.id).not.toBe(first.content[0].trip.id);
    for (const { trip, firstDays } of first.content) {
      expect(trip.name).toBeTruthy();
      expect(trip.budget).not.toBeNull();
      expect(firstDays.length).toBeGreaterThan(0);
      expect(firstDays.length).toBeLessThanOrEqual(3);
      expect(firstDays.map((preview) => preview.day)).toEqual(
        [...firstDays.map((preview) => preview.day)].sort(),
      );
      expect(firstDays.every((preview) => preview.day >= 1 && preview.day <= 3)).toBe(true);
      expect(firstDays.every((preview) => preview.activityName.length > 0)).toBe(true);
    }
  });

  test('an anonymous visitor opens an example trip and browses its days', async () => {
    const { trip } = (await getExampleTrips(0, 3)).content[0];

    await expect(getExampleTrip(trip.id)).resolves.toEqual(trip);
    const day = await getExampleActivities(trip.id, trip.startDate);

    expect(day.content.length).toBeGreaterThan(0);
    expect(day.content.every((activity) => activity.date === trip.startDate)).toBe(true);
    expect(day.content.every((activity) => activity.tripId === trip.id)).toBe(true);
    const all = await getExampleActivities(trip.id);
    expect(all.totalElements).toBeGreaterThanOrEqual(day.totalElements);
  });

  test('an example trip that does not exist is a 404', async () => {
    await expect(getExampleTrip(999999999)).rejects.toMatchObject({ status: 404 });
    await expect(getExampleActivities(999999999)).rejects.toMatchObject({ status: 404 });
  });

  test("a regular user's trip is a 404 on the public endpoints, never exposed as an example", async () => {
    await newUser();
    const own = await createTrip({
      name: 'Privado',
      destination: 'Lisboa',
      startDate: '2027-05-01',
      endDate: '2027-05-05',
      budget: null,
      description: null,
    });

    await expect(getExampleTrip(own.id)).rejects.toMatchObject({ status: 404 });
    await expect(getExampleActivities(own.id)).rejects.toMatchObject({ status: 404 });
    const listed = await getExampleTrips(0, 50);
    expect(listed.content.map((example) => example.trip.id)).not.toContain(own.id);
  });
});
