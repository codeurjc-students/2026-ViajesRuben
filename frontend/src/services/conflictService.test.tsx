import { afterEach, describe, expect, test, vi } from 'vitest';
import { conflictsOf } from '../testHelpers';
import { getConflicts } from './conflictService';

afterEach(() => {
  vi.restoreAllMocks();
});

describe('B6 conflict service', () => {
  test('getConflicts reads the conflicts of the trip', async () => {
    const conflicts = conflictsOf({ budget: { totalCost: 120, budget: 100 } });
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve(conflicts),
    } as Response);

    await expect(getConflicts(5)).resolves.toEqual(conflicts);

    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(String(url)).toBe('/api/v1/trips/5/conflicts');
    expect(init).toBeUndefined();
  });

  test.each([401, 403, 404])('turns a %i into an ApiError', async (status) => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({ ok: false, status } as Response);

    await expect(getConflicts(5)).rejects.toMatchObject({ name: 'ApiError', status });
  });
});
