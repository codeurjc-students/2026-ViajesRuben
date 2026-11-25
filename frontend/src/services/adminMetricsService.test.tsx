import { afterEach, describe, expect, test, vi } from 'vitest';
import { getMetrics } from './adminMetricsService';
import { ApiError } from './apiError';

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

describe('I6 adminMetricsService', () => {
  test('getMetrics reads the totals from the admin metrics URL', async () => {
    const metrics = { users: 3, trips: 5, activities: 12 };
    answer(200, metrics);

    await expect(getMetrics()).resolves.toEqual(metrics);

    expect(vi.mocked(fetch).mock.calls[0][0]).toContain('/api/v1/admin/metrics');
  });

  test.each([401, 403])('getMetrics turns a %i answer into an ApiError', async (status) => {
    answer(status);

    const result = getMetrics();

    await expect(result).rejects.toBeInstanceOf(ApiError);
    await expect(result).rejects.toMatchObject({ status });
  });
});
