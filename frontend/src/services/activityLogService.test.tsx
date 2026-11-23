import { afterEach, describe, expect, test, vi } from 'vitest';
import { logEntryOf, pageOf } from '../testHelpers';
import { getActivityLog } from './activityLogService';

afterEach(() => {
  vi.restoreAllMocks();
});

function answer(body: unknown, status = 200) {
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({
    ok: true,
    status,
    json: () => Promise.resolve(body),
  } as Response);
}

describe('I4 activityLogService', () => {
  test('getActivityLog asks for the first page of the trip by default', async () => {
    const page = pageOf([logEntryOf({ action: 'UPDATED', changedFields: ['name', 'date'] })]);
    answer(page);

    await expect(getActivityLog(3)).resolves.toEqual(page);

    expect(vi.mocked(fetch).mock.calls[0][0]).toContain(
      '/api/v1/trips/3/activity-log?page=0&size=10',
    );
  });

  test('getActivityLog asks for the requested page and size', async () => {
    answer(pageOf([]));

    await getActivityLog(3, 2, 5);

    expect(vi.mocked(fetch).mock.calls[0][0]).toContain(
      '/api/v1/trips/3/activity-log?page=2&size=5',
    );
  });

  test.each([401, 403, 404])(
    'getActivityLog fails with the status %i of the server',
    async (status) => {
      vi.spyOn(globalThis, 'fetch').mockResolvedValue({
        ok: false,
        status,
        json: () => Promise.resolve({}),
      } as Response);

      await expect(getActivityLog(3)).rejects.toMatchObject({ status });
    },
  );
});
