import { afterEach, describe, expect, test, vi } from 'vitest';
import { pageOf, publicTripOf } from '../testHelpers';
import { getPublicActivities, getPublicTrip, getPublicTrips } from './publicTripService';

afterEach(() => {
  vi.restoreAllMocks();
});

function answer(body: unknown) {
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({
    ok: true,
    json: () => Promise.resolve(body),
  } as Response);
}

describe('B1 I7 publicTripService', () => {
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
