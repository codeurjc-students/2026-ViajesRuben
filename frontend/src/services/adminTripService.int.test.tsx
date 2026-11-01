// @vitest-environment node
import { afterEach, describe, expect, test, vi } from 'vitest';
import { newUser, sendCookie, sessionCookie } from '../integrationHelpers';
import { getTrips } from './adminTripService';
import { createTrip, deleteTrip, getTrip, updateTrip } from './tripService';

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

/** A trip with a unique name, because the development database keeps the trips of earlier runs. */
async function newTrip() {
  const name = `Moderación ${Date.now()}-${Math.floor(Math.random() * 1e6)}`;
  const trip = await createTrip({
    name,
    destination: 'Moderlandia',
    startDate: '2027-05-01',
    endDate: '2027-05-05',
    budget: null,
    description: null,
  });
  return { trip, name };
}

describe('B8 adminTripService against the REST API', () => {
  test('without a session the list is a 401', async () => {
    await expect(getTrips('')).rejects.toMatchObject({ status: 401 });
  });

  test('a regular user gets 403 on the list', async () => {
    await newUser();

    await expect(getTrips('')).rejects.toMatchObject({ status: 403 });
  });

  test('the administrator finds a trip of another user by name, destination or creator email', async () => {
    const { email } = await newUser();
    const { trip, name } = await newTrip();
    await asAdmin();

    const byName = await getTrips(name.toLowerCase());
    const byEmail = await getTrips(email.toUpperCase());

    expect(byName.content).toEqual([
      {
        id: trip.id,
        name,
        destination: 'Moderlandia',
        startDate: '2027-05-01',
        endDate: '2027-05-05',
        creatorId: trip.creatorId,
        creatorEmail: email,
      },
    ]);
    expect(byEmail.content.map((found) => found.id)).toEqual([trip.id]);
    expect((await getTrips('no-trip-has-this-text-7f3a')).content).toEqual([]);
  });

  test('the list is paginated', async () => {
    await newUser();
    await newTrip();
    await newTrip();
    await asAdmin();

    const first = await getTrips('Moderlandia', 0, 1);
    const second = await getTrips('Moderlandia', 1, 1);

    expect(first.content).toHaveLength(1);
    expect(first.size).toBe(1);
    expect(first.totalElements).toBeGreaterThanOrEqual(2);
    expect(second.content).toHaveLength(1);
    expect(second.content[0].id).not.toBe(first.content[0].id);
  });

  test('the administrator reads and deletes another user trip but cannot edit it', async () => {
    await newUser();
    const { trip, name } = await newTrip();
    await asAdmin();

    await expect(getTrip(trip.id)).resolves.toMatchObject({ id: trip.id, name });
    await expect(
      updateTrip(trip.id, {
        name: 'Cambiado',
        destination: 'Otro',
        startDate: '2027-05-01',
        endDate: '2027-05-05',
        budget: null,
        description: null,
      }),
    ).rejects.toMatchObject({ status: 403 });

    await expect(deleteTrip(trip.id)).resolves.toBeUndefined();

    await expect(getTrip(trip.id)).rejects.toMatchObject({ status: 404 });
    expect((await getTrips(name)).content).toEqual([]);
  });

  test('a regular user cannot read or delete the trip of another user', async () => {
    await newUser();
    const { trip } = await newTrip();
    // A second spy on fetch would call itself, so drop Ana's cookie before Bob's.
    vi.restoreAllMocks();
    await newUser('Bob');

    await expect(getTrip(trip.id)).rejects.toMatchObject({ status: 403 });
    await expect(deleteTrip(trip.id)).rejects.toMatchObject({ status: 403 });
  });
});
