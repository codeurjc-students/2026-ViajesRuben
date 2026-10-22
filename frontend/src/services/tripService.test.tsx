import { test, expect, vi, afterEach } from 'vitest';
import { getTrips } from './tripService';
import { ApiError, NetworkError } from './apiError';

afterEach(() => {
  vi.restoreAllMocks();
});

test('fetches trips from the REST API', async () => {
  const trips = [
    {
      id: 1,
      name: 'Trip 1',
      destination: 'Lisbon',
      startDate: '2027-05-01',
      endDate: '2027-05-05',
    },
  ];
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({
    ok: true,
    json: () => Promise.resolve(trips),
  } as Response);

  const result = await getTrips();

  expect(vi.mocked(fetch).mock.calls[0][0]).toContain('/api/v1/trips');
  expect(result).toEqual(trips);
});

test('turns a non-2xx answer into an ApiError with the status', async () => {
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({ ok: false, status: 503 } as Response);

  await expect(getTrips()).rejects.toMatchObject({ name: 'ApiError', status: 503 });
  await expect(getTrips()).rejects.toBeInstanceOf(ApiError);
});

test('turns a failed connection into a NetworkError', async () => {
  vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('Failed to fetch'));

  await expect(getTrips()).rejects.toBeInstanceOf(NetworkError);
});
