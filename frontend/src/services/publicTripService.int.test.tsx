// @vitest-environment node
import { afterEach, describe, expect, test, vi } from 'vitest';
import { newUser, sendCookie } from '../integrationHelpers';
import { createActivity, getActivities } from './activityService';
import {
  copyPublicTrip,
  getPublicActivities,
  getPublicTrip,
  getPublicTrips,
} from './publicTripService';
import { createTrip, deleteTrip, getTrip, setPublication } from './tripService';

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
describe('B1 I7 I8 publicTripService against the REST API', () => {
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

  test('a search finds a public trip by name or destination ignoring case, and never an unpublished one', async () => {
    const { cookie } = await newUser('Buscadora');
    const unique = `Zzq${Date.now()}`;
    const published = await createTrip({
      ...NEW_TRIP,
      name: `${unique} visible`,
      destination: `${unique}City`,
    });
    const hidden = await createTrip({
      ...NEW_TRIP,
      name: `${unique} oculto`,
      destination: `${unique}City`,
    });
    await setPublication(published.id, true);

    try {
      vi.restoreAllMocks();
      const byName = await getPublicTrips(0, 10, `  ${unique.toUpperCase()} VISIBLE `);
      const byDestination = await getPublicTrips(0, 10, `${unique.toLowerCase()}CITY`);

      expect(byName.content.map((summary) => summary.trip.id)).toEqual([published.id]);
      expect(byDestination.content.map((summary) => summary.trip.id)).toEqual([published.id]);
      expect(byDestination.content.map((summary) => summary.trip.id)).not.toContain(hidden.id);
      expect(JSON.stringify(byDestination)).not.toMatch(/@|email|participants/i);
    } finally {
      // The public trips are shared with the other tests (the newest ones must have previews).
      sendCookie(cookie);
      await setPublication(published.id, false);
    }
  });

  test('a search without matches is an empty page and a blank search lists every public trip', async () => {
    const none = await getPublicTrips(0, 10, 'no-trip-has-this-text-8f3a');
    const blank = await getPublicTrips(0, 10, '   ');
    const all = await getPublicTrips(0, 10);

    expect(none).toMatchObject({ totalElements: 0, last: true });
    expect(none.content).toEqual([]);
    expect(blank.totalElements).toBe(all.totalElements);
  });

  test('the search is paginated', async () => {
    const first = await getPublicTrips(0, 1, 'a');
    const second = await getPublicTrips(1, 1, 'a');

    expect(first).toMatchObject({ page: 0, size: 1 });
    expect(first.content).toHaveLength(1);
    expect(second.page).toBe(1);
    expect(second.content[0].trip.id).not.toBe(first.content[0].trip.id);
  });
});

/** Node keeps no cookies and `sendCookie` cannot wrap itself: drop the previous user first. */
async function signUp(name: string) {
  vi.restoreAllMocks();
  return newUser(name);
}

function actAs(cookie: string) {
  vi.restoreAllMocks();
  sendCookie(cookie);
}

describe('I9 copyPublicTrip against the REST API', () => {
  const ACTIVITY = {
    name: 'Torre de Belém',
    date: '2027-05-02',
    startTime: '10:00',
    endTime: '12:00',
    location: null,
    description: null,
    cost: 10,
    category: null,
  };

  // The public trips are shared with the other tests: never leave one published, even on failure.
  const published: { cookie: string; id: number }[] = [];

  afterEach(async () => {
    for (const { cookie, id } of published.splice(0)) {
      actAs(cookie);
      await setPublication(id, false).catch(() => undefined); // already deleted
    }
  });

  /** A published trip with one activity, made by a new author; leaves the author's cookie active. */
  async function publishedTrip() {
    const author = await signUp('Autora');
    const original = await createTrip({ ...NEW_TRIP, name: `Copiable ${Date.now()}` });
    await createActivity(original.id, ACTIVITY);
    await setPublication(original.id, true);
    published.push({ cookie: author.cookie, id: original.id });
    return { author, original };
  }

  test('a logged-in user copies a public trip into an unpublished trip of their own with its activities', async () => {
    const { original } = await publishedTrip();
    const copier = await signUp('Copiadora');

    const copy = await copyPublicTrip(original.id);

    expect(copy.id).not.toBe(original.id);
    expect(copy).toMatchObject({
      creatorId: copier.user.id,
      name: original.name,
      destination: original.destination,
      startDate: '2027-05-01',
      endDate: '2027-05-05',
      published: false,
      publishedAt: null,
      copiedFromId: original.id,
    });
    await expect(getTrip(copy.id)).resolves.toMatchObject({ copiedFromId: original.id });
    const activities = await getActivities(copy.id);
    expect(activities.content).toHaveLength(1);
    expect(activities.content[0]).toMatchObject({
      name: ACTIVITY.name,
      date: '2027-05-02',
      authorId: copier.user.id,
    });

    await deleteTrip(copy.id);
  });

  test('a start date moves the trip and its activities by the same number of days', async () => {
    const { original } = await publishedTrip();
    await signUp('Copiadora');

    const copy = await copyPublicTrip(original.id, '2027-06-01');

    expect(copy).toMatchObject({ startDate: '2027-06-01', endDate: '2027-06-05' });
    const activities = await getActivities(copy.id);
    expect(activities.content.map((activity) => activity.date)).toEqual(['2027-06-02']);

    await deleteTrip(copy.id);
  });

  test('deleting the original leaves the copy with its activities and no link', async () => {
    const { author, original } = await publishedTrip();
    const copier = await signUp('Copiadora');
    const copy = await copyPublicTrip(original.id);

    actAs(author.cookie);
    await deleteTrip(original.id);
    actAs(copier.cookie);

    await expect(getTrip(copy.id)).resolves.toMatchObject({ copiedFromId: null });
    await expect(getActivities(copy.id)).resolves.toMatchObject({ totalElements: 1 });
    await deleteTrip(copy.id);
  });

  test('an anonymous visitor cannot copy a public trip: 401', async () => {
    const { original } = await publishedTrip();
    vi.restoreAllMocks();

    await expect(copyPublicTrip(original.id)).rejects.toMatchObject({ status: 401 });
  });

  test('an unpublished or unknown trip cannot be copied: 404', async () => {
    await signUp('Autora');
    const draft = await createTrip(NEW_TRIP);

    await expect(copyPublicTrip(draft.id)).rejects.toMatchObject({ status: 404 });
    await expect(copyPublicTrip(999999999)).rejects.toMatchObject({ status: 404 });
  });
});
