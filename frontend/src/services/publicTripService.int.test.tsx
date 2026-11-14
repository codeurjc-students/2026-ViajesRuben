// @vitest-environment node
import { afterEach, describe, expect, test, vi } from 'vitest';
import { newUser, sendCookie } from '../integrationHelpers';
import { getPublicActivities, getPublicTrip, getPublicTrips } from './publicTripService';
import { createTrip, setPublication } from './tripService';

afterEach(() => {
  vi.restoreAllMocks();
});

const NEW_TRIP = {
  name: 'Viaje público de prueba',
  destination: 'Lisboa',
  startDate: '2027-05-01',
  endDate: '2027-05-05',
  budget: null,
  description: null,
};

// The first public trips are the sample data (SAMPLE_DATA, on by default): the administrator's
// three, each with an itinerary on every day, and one of two other authors.
describe('B1 I7 publicTripService against the REST API', () => {
  test('an anonymous visitor lists the public trips with a preview of the first days', async () => {
    const all = await getPublicTrips(0, 3);

    expect(all).toMatchObject({ page: 0, size: 3 });
    expect(all.totalElements).toBeGreaterThanOrEqual(3);
    expect(all.content).toHaveLength(3);
    for (const { trip, firstDays } of all.content) {
      expect(trip.name).toBeTruthy();
      expect(trip.authorName).toBeTruthy();
      expect(firstDays.length).toBeGreaterThan(0);
      expect(firstDays.length).toBeLessThanOrEqual(3);
      expect(firstDays.every((preview) => preview.day >= 1 && preview.day <= 3)).toBe(true);
      expect(firstDays.every((preview) => preview.activityName.length > 0)).toBe(true);
    }
  });

  test('the public list is paginated and the newest publication comes first', async () => {
    const first = await getPublicTrips(0, 2);
    const second = await getPublicTrips(1, 2);

    expect(first).toMatchObject({ page: 0, size: 2, last: false });
    expect(first.content).toHaveLength(2);
    expect(second.page).toBe(1);
    expect(second.content[0].trip.id).not.toBe(first.content[0].trip.id);
    expect(second.content[0].trip.id).not.toBe(first.content[1].trip.id);
    const dates = [...first.content, ...second.content].map((s) => s.trip.publishedAt);
    expect(dates).toEqual([...dates].sort().reverse());
  });

  test('the public responses never contain an email', async () => {
    const all = await getPublicTrips(0, 50);
    const one = await getPublicTrip(all.content[0].trip.id);

    expect(JSON.stringify(all)).not.toMatch(/@|email|participants/i);
    expect(JSON.stringify(one)).not.toMatch(/@|email|participants/i);
  });

  test('an anonymous visitor opens a public trip and browses its days', async () => {
    const { trip } = (await getPublicTrips(0, 3)).content[0];

    await expect(getPublicTrip(trip.id)).resolves.toEqual(trip);
    const day = await getPublicActivities(trip.id, trip.startDate);

    expect(day.content.length).toBeGreaterThan(0);
    expect(day.content.every((activity) => activity.date === trip.startDate)).toBe(true);
    expect(day.content.every((activity) => activity.tripId === trip.id)).toBe(true);
    const all = await getPublicActivities(trip.id);
    expect(all.totalElements).toBeGreaterThanOrEqual(day.totalElements);
  });

  test('a public trip that does not exist is a 404', async () => {
    await expect(getPublicTrip(999999999)).rejects.toMatchObject({ status: 404 });
    await expect(getPublicActivities(999999999)).rejects.toMatchObject({ status: 404 });
  });

  test('a trip is a 404 on the public endpoints until its creator publishes it, and again after unpublishing it', async () => {
    const { cookie } = await newUser('Autora');
    const own = await createTrip(NEW_TRIP);
    expect(own).toMatchObject({ published: false, publishedAt: null });

    // Anonymous: nothing is public yet.
    vi.restoreAllMocks();
    await expect(getPublicTrip(own.id)).rejects.toMatchObject({ status: 404 });
    await expect(getPublicActivities(own.id)).rejects.toMatchObject({ status: 404 });

    sendCookie(cookie);
    const published = await setPublication(own.id, true);
    expect(published).toMatchObject({ published: true });
    expect(published.publishedAt).not.toBeNull();

    // Anonymous: the trip is the newest public itinerary, with the author's name.
    vi.restoreAllMocks();
    const newest = (await getPublicTrips(0, 3)).content[0];
    expect(newest.trip).toMatchObject({ id: own.id, authorName: 'Autora' });
    await expect(getPublicTrip(own.id)).resolves.toMatchObject({ name: NEW_TRIP.name });
    await expect(getPublicActivities(own.id)).resolves.toMatchObject({ totalElements: 0 });

    sendCookie(cookie);
    await expect(setPublication(own.id, false)).resolves.toMatchObject({
      published: false,
      publishedAt: null,
    });

    vi.restoreAllMocks();
    await expect(getPublicTrip(own.id)).rejects.toMatchObject({ status: 404 });
    const listed = await getPublicTrips(0, 50);
    expect(listed.content.map((summary) => summary.trip.id)).not.toContain(own.id);
  });

  test('another user cannot publish a trip', async () => {
    await newUser('Autora');
    const own = await createTrip(NEW_TRIP);
    vi.restoreAllMocks();
    await newUser('Intrusa');

    await expect(setPublication(own.id, true)).rejects.toMatchObject({ status: 403 });
  });
});
