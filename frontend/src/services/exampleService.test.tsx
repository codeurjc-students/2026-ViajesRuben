import { afterEach, describe, expect, test, vi } from 'vitest';
import { exampleOf, pageOf, tripOf } from '../testHelpers';
import { getExampleActivities, getExampleTrip, getExampleTrips } from './exampleService';

afterEach(() => {
  vi.restoreAllMocks();
});

function answer(body: unknown) {
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({
    ok: true,
    json: () => Promise.resolve(body),
  } as Response);
}

describe('B1 exampleService', () => {
  test('asks for three example trips at a time by default', async () => {
    const page = pageOf([exampleOf()]);
    answer(page);

    await expect(getExampleTrips()).resolves.toEqual(page);

    expect(vi.mocked(fetch).mock.calls[0][0]).toContain('/api/v1/trips/examples?page=0&size=3');
  });

  test('asks for the requested page of example trips', async () => {
    answer(pageOf([]));

    await getExampleTrips(1, 3);

    expect(vi.mocked(fetch).mock.calls[0][0]).toContain('/api/v1/trips/examples?page=1&size=3');
  });

  test('fetches one example trip', async () => {
    const trip = tripOf({ id: 7 });
    answer(trip);

    await expect(getExampleTrip(7)).resolves.toEqual(trip);

    expect(vi.mocked(fetch).mock.calls[0][0]).toContain('/api/v1/trips/examples/7');
  });

  test('fetches the activities of one day of an example trip', async () => {
    answer(pageOf([]));

    await getExampleActivities(7, '2027-05-02', 1);

    expect(vi.mocked(fetch).mock.calls[0][0]).toContain(
      '/api/v1/trips/examples/7/activities?page=1&size=10&date=2027-05-02',
    );
  });

  test('fetches all the activities when there is no day', async () => {
    answer(pageOf([]));

    await getExampleActivities(7);

    expect(vi.mocked(fetch).mock.calls[0][0]).toMatch(/activities\?page=0&size=10$/);
  });

  test('a trip that is not an example is an ApiError 404', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({ ok: false, status: 404 } as Response);

    await expect(getExampleTrip(1)).rejects.toMatchObject({ name: 'ApiError', status: 404 });
    await expect(getExampleActivities(1)).rejects.toMatchObject({ status: 404 });
  });
});
