import { afterEach, describe, expect, test, vi } from 'vitest';
import { getTrips } from './adminTripService';
import { ApiError } from './apiError';
import { adminTripOf, pageOf } from '../testHelpers';

function answer(status: number, body?: unknown) {
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  } as Response);
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe('B8 adminTripService', () => {
  test('getTrips asks for a page without a search when the text is blank', async () => {
    answer(200, pageOf([adminTripOf()]));

    await getTrips('   ', 2, 5);

    expect(vi.mocked(fetch).mock.calls[0][0]).toContain('/api/v1/admin/trips?page=2&size=5');
    expect(vi.mocked(fetch).mock.calls[0][0]).not.toContain('search');
  });

  test('getTrips sends the trimmed search text, encoded', async () => {
    answer(200, pageOf([adminTripOf()]));

    await expect(getTrips(' ana@example.com&x ')).resolves.toEqual(pageOf([adminTripOf()]));

    expect(vi.mocked(fetch).mock.calls[0][0]).toContain('search=ana%40example.com%26x');
    expect(vi.mocked(fetch).mock.calls[0][0]).toContain('page=0&size=10');
  });

  test('getTrips turns a non-administrator into an ApiError 403', async () => {
    answer(403);

    await expect(getTrips('')).rejects.toBeInstanceOf(ApiError);
    await expect(getTrips('')).rejects.toMatchObject({ status: 403 });
  });
});
