import { renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { getMetrics } from '../services/adminMetricsService';
import { ApiError } from '../services/apiError';
import { usePlatformMetrics } from './usePlatformMetrics';

vi.mock('../services/adminMetricsService', () => ({ getMetrics: vi.fn() }));

beforeEach(() => {
  vi.mocked(getMetrics).mockReset();
});

describe('I6 usePlatformMetrics', () => {
  test('loads the totals once', async () => {
    const metrics = { users: 3, trips: 5, activities: 12 };
    vi.mocked(getMetrics).mockResolvedValue(metrics);

    const { result } = renderHook(() => usePlatformMetrics());

    expect(result.current.metrics).toBeUndefined();
    await waitFor(() => expect(result.current.metrics).toEqual(metrics));
    expect(result.current.error).toBeUndefined();
    expect(getMetrics).toHaveBeenCalledOnce();
  });

  test('keeps the error when the totals cannot be loaded', async () => {
    const failure = new ApiError(500);
    vi.mocked(getMetrics).mockRejectedValue(failure);

    const { result } = renderHook(() => usePlatformMetrics());

    await waitFor(() => expect(result.current.error).toBe(failure));
    expect(result.current.metrics).toBeUndefined();
  });
});
