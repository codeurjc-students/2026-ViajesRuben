import { afterEach, describe, expect, test, vi } from 'vitest';
import { pageOf, publicTripOf, tripOf } from '../testHelpers';
import {
  copyPublicTrip,
  getPublicActivities,
  getPublicTrip,
  getPublicTrips,
} from './publicTripService';

afterEach(() => {
  vi.restoreAllMocks();
});

function answer(body: unknown) {
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({
    ok: true,
    json: () => Promise.resolve(body),
  } as Response);
}

describe('B1 I7 I8 publicTripService', () => {
  test('searches by the trimmed, encoded text', async () => {
    answer(pageOf([]));

    await getPublicTrips(0, 10, '  Río & Co ');

    expect(vi.mocked(fetch).mock.calls[0][0]).toContain(
      '/api/v1/trips/public?page=0&size=10&search=R%C3%ADo%20%26%20Co',
    );
  });

  test('sends no search parameter for a blank text', async () => {
    answer(pageOf([]));

    await getPublicTrips(0, 10, '   ');

    expect(vi.mocked(fetch).mock.calls[0][0]).toMatch(/\/public\?page=0&size=10$/);
  });

  test('asks for three public trips at a time by default', async () => {
    const page = pageOf([publicTripOf()]);
    answer(page);

    await expect(getPublicTrips()).resolves.toEqual(page);

    expect(vi.mocked(fetch).mock.calls[0][0]).toContain('/api/v1/trips/public?page=0&size=3');
  });

  test('asks for the requested page of public trips', async () => {
    answer(pageOf([]));

    await getPublicTrips(1, 3);

    expect(vi.mocked(fetch).mock.calls[0][0]).toContain('/api/v1/trips/public?page=1&size=3');
  });

  test('fetches one public trip', async () => {
    const trip = publicTripOf().trip;
    answer(trip);

    await expect(getPublicTrip(7)).resolves.toEqual(trip);

    expect(vi.mocked(fetch).mock.calls[0][0]).toContain('/api/v1/trips/public/7');
  });

  test('fetches the activities of one day of a public trip', async () => {
    answer(pageOf([]));

    await getPublicActivities(7, '2027-05-02', 1);

    expect(vi.mocked(fetch).mock.calls[0][0]).toContain(
      '/api/v1/trips/public/7/activities?page=1&size=10&date=2027-05-02',
    );
  });

  test('fetches all the activities when there is no day', async () => {
    answer(pageOf([]));

    await getPublicActivities(7);

    expect(vi.mocked(fetch).mock.calls[0][0]).toMatch(/activities\?page=0&size=10$/);
  });

  test('a trip that is not published is an ApiError 404', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({ ok: false, status: 404 } as Response);

    await expect(getPublicTrip(1)).rejects.toMatchObject({ name: 'ApiError', status: 404 });
    await expect(getPublicActivities(1)).rejects.toMatchObject({ status: 404 });
  });
});

describe('I9 copyPublicTrip', () => {
  test('posts the start date to the copies of the public trip and returns the new trip', async () => {
    const copy = tripOf({ id: 20, copiedFromId: 7 });
    answer(copy);

    await expect(copyPublicTrip(7, '2027-09-01')).resolves.toEqual(copy);

    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(url).toContain('/api/v1/trips/public/7/copies');
    expect(init?.method).toBe('POST');
    expect(init?.body).toBe(JSON.stringify({ startDate: '2027-09-01' }));
  });

  test('sends an empty body when there is no start date, to keep the original dates', async () => {
    answer(tripOf({ id: 20, copiedFromId: 7 }));

    await copyPublicTrip(7);
    await copyPublicTrip(7, '');

    expect(vi.mocked(fetch).mock.calls[0][1]?.body).toBe('{}');
    expect(vi.mocked(fetch).mock.calls[1][1]?.body).toBe('{}');
  });

  test.each([401, 404, 400])('a %i answer is an ApiError', async (status) => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({ ok: false, status } as Response);

    await expect(copyPublicTrip(7)).rejects.toMatchObject({ name: 'ApiError', status });
  });
});
