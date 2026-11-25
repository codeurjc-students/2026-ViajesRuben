// @vitest-environment node
import { afterEach, describe, expect, test, vi } from 'vitest';
import { newUser, sendCookie, sessionCookie } from '../integrationHelpers';
import { getMetrics } from './adminMetricsService';
import { createTrip } from './tripService';

// The development administrator (ADMIN_EMAIL / ADMIN_PASSWORD_HASH defaults), as in CI.
const ADMIN_EMAIL = 'admin@tripmate.com';
const ADMIN_PASSWORD = 'admin';

afterEach(() => {
  vi.restoreAllMocks();
});

async function asAdmin() {
  vi.restoreAllMocks();
  sendCookie(await sessionCookie(ADMIN_EMAIL, ADMIN_PASSWORD));
}

describe('I6 adminMetricsService against the REST API', () => {
  test('without a session the totals are a 401', async () => {
    await expect(getMetrics()).rejects.toMatchObject({ status: 401 });
  });

  test('a regular user gets 403', async () => {
    await newUser();

    await expect(getMetrics()).rejects.toMatchObject({ status: 403 });
  });

  test('the administrator reads the three totals as numbers', async () => {
    await asAdmin();

    const metrics = await getMetrics();

    expect(Object.keys(metrics).sort()).toEqual(['activities', 'trips', 'users']);
    expect(Number.isInteger(metrics.users)).toBe(true);
    expect(Number.isInteger(metrics.trips)).toBe(true);
    expect(Number.isInteger(metrics.activities)).toBe(true);
  });

  // The integration files run in parallel against the same backend, so the totals can grow by
  // more than this test's own rows: the exact deltas are checked by the sequential REST API test.
  test('a new user and a new trip are counted', async () => {
    await asAdmin();
    const before = await getMetrics();

    // A second spy on fetch would call itself, so drop the administrator's cookie first.
    vi.restoreAllMocks();
    await newUser();
    await createTrip({
      name: `Métricas ${Date.now()}-${Math.floor(Math.random() * 1e6)}`,
      destination: 'Cifras',
      startDate: '2027-05-01',
      endDate: '2027-05-05',
      budget: null,
      description: null,
    });
    await asAdmin();
    const after = await getMetrics();

    expect(after.users).toBeGreaterThanOrEqual(before.users + 1);
    expect(after.trips).toBeGreaterThanOrEqual(before.trips + 1);
    expect(after.activities).toBeGreaterThanOrEqual(before.activities);
  });
});
