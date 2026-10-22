import { test, expect, vi, afterEach } from 'vitest';
import { getTrips } from './tripService';
import { pageOf, tripsFrom } from '../testHelpers';
import { ApiError, NetworkError } from './apiError';

afterEach(() => {
  vi.restoreAllMocks();
});

test('fetches the first page of trips from the REST API', async () => {
  const page = pageOf(tripsFrom(1, 1));
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({
    ok: true,
    json: () => Promise.resolve(page),
  } as Response);

  const result = await getTrips();

  expect(vi.mocked(fetch).mock.calls[0][0]).toContain('/api/v1/trips?page=0&size=10');
  expect(result).toEqual(page);
});

test('asks for the requested page and size', async () => {
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({
    ok: true,
    json: () => Promise.resolve(pageOf([])),
  } as Response);

  await getTrips(2, 5);

  expect(vi.mocked(fetch).mock.calls[0][0]).toContain('/api/v1/trips?page=2&size=5');
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
