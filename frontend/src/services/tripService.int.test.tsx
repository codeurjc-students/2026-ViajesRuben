// @vitest-environment node
// Node's fetch needs Node's own FormData and File for the multipart upload, not the jsdom ones.
import { afterEach, describe, expect, test, vi } from 'vitest';
import { newUser, png } from '../integrationHelpers';
import { imageUrl } from './imageService';
import {
  applyCover,
  createTrip,
  deleteCover,
  deleteTrip,
  getTrip,
  getTrips,
  updateTrip,
  uploadCover,
} from './tripService';

const data = {
  name: 'Escapada',
  destination: 'Lisboa',
  startDate: '2027-05-01',
  endDate: '2027-05-05',
  budget: 1200.5,
  description: 'Pastéis',
};

afterEach(() => {
  vi.restoreAllMocks();
});

describe('B4 tripService against the REST API', () => {
  test('without a session every trip operation is a 401', async () => {
    await expect(getTrips()).rejects.toMatchObject({ status: 401 });
    await expect(getTrip(1)).rejects.toMatchObject({ status: 401 });
    await expect(createTrip(data)).rejects.toMatchObject({ status: 401 });
    await expect(updateTrip(1, data)).rejects.toMatchObject({ status: 401 });
    await expect(deleteTrip(1)).rejects.toMatchObject({ status: 401 });
    await expect(uploadCover(1, png())).rejects.toMatchObject({ status: 401 });
    await expect(deleteCover(1)).rejects.toMatchObject({ status: 401 });
  });

  test('creates, reads, updates and deletes a trip', async () => {
    const { user } = await newUser();

    const created = await createTrip(data);
    expect(created).toMatchObject({ ...data, creatorId: user.id, coverImageId: null });
    await expect(getTrip(created.id)).resolves.toEqual(created);

    const updated = await updateTrip(created.id, {
      ...data,
      name: 'Oslo',
      budget: null,
      description: null,
    });
    expect(updated).toMatchObject({ id: created.id, name: 'Oslo', budget: null });
    await expect(getTrip(created.id)).resolves.toMatchObject({ name: 'Oslo', description: null });

    await expect(deleteTrip(created.id)).resolves.toBeUndefined();
    await expect(getTrip(created.id)).rejects.toMatchObject({ status: 404 });
  });

  test('the list holds only the own trips, one page at a time', async () => {
    await newUser();
    for (let i = 1; i <= 3; i++) await createTrip({ ...data, name: `Trip ${i}` });

    const first = await getTrips(0, 2);
    const second = await getTrips(1, 2);

    expect(first).toMatchObject({ page: 0, size: 2, totalElements: 3, last: false });
    expect(first.content.map((trip) => trip.name)).toEqual(['Trip 1', 'Trip 2']);
    expect(second.content.map((trip) => trip.name)).toEqual(['Trip 3']);
    expect(second.last).toBe(true);
  });

  test('the end date before the start date is a 400 and nothing is created', async () => {
    await newUser();

    await expect(createTrip({ ...data, endDate: '2027-04-01' })).rejects.toMatchObject({
      status: 400,
    });
    await expect(createTrip({ ...data, name: ' ' })).rejects.toMatchObject({ status: 400 });
    await expect(getTrips()).resolves.toMatchObject({ totalElements: 0 });
  });

  test('another user cannot read, change, delete or cover a trip: 403', async () => {
    await newUser('Ana');
    const trip = await createTrip(data);
    vi.restoreAllMocks();
    await newUser('Bob');

    await expect(getTrip(trip.id)).rejects.toMatchObject({ status: 403 });
    await expect(updateTrip(trip.id, data)).rejects.toMatchObject({ status: 403 });
    await expect(deleteTrip(trip.id)).rejects.toMatchObject({ status: 403 });
    await expect(uploadCover(trip.id, png())).rejects.toMatchObject({ status: 403 });
    await expect(deleteCover(trip.id)).rejects.toMatchObject({ status: 403 });
    await expect(getTrips()).resolves.toMatchObject({ totalElements: 0 });
  });

  test('a trip that does not exist is a 404', async () => {
    await newUser();

    await expect(getTrip(999_999_999)).rejects.toMatchObject({ status: 404 });
    await expect(updateTrip(999_999_999, data)).rejects.toMatchObject({ status: 404 });
    await expect(deleteTrip(999_999_999)).rejects.toMatchObject({ status: 404 });
    await expect(uploadCover(999_999_999, png())).rejects.toMatchObject({ status: 404 });
  });

  test('uploads, replaces and deletes the cover, deleting the old pictures', async () => {
    await newUser();
    const trip = await createTrip(data);

    const first = await uploadCover(trip.id, png());
    expect((await getTrip(trip.id)).coverImageId).toBe(first.id);
    expect((await fetch(imageUrl(first.id))).status).toBe(200);

    const second = await uploadCover(trip.id, png());
    expect((await getTrip(trip.id)).coverImageId).toBe(second.id);
    expect((await fetch(imageUrl(first.id))).status).toBe(404);

    await deleteCover(trip.id);
    expect((await getTrip(trip.id)).coverImageId).toBeNull();
    expect((await fetch(imageUrl(second.id))).status).toBe(404);
  });

  test('a cover that is not an image is a 400 and keeps the current one', async () => {
    await newUser();
    const trip = await createTrip(data);
    const cover = await uploadCover(trip.id, png());

    await expect(
      uploadCover(trip.id, new File(['hello'], 'a.png', { type: 'image/png' })),
    ).rejects.toMatchObject({ status: 400 });

    expect((await getTrip(trip.id)).coverImageId).toBe(cover.id);
  });

  test('applyCover replaces and removes the cover', async () => {
    await newUser();
    const trip = await createTrip(data);

    await applyCover(trip.id, { kind: 'replace', file: png() });
    expect((await getTrip(trip.id)).coverImageId).not.toBeNull();
    await applyCover(trip.id, { kind: 'keep' });
    expect((await getTrip(trip.id)).coverImageId).not.toBeNull();
    await applyCover(trip.id, { kind: 'remove' });
    expect((await getTrip(trip.id)).coverImageId).toBeNull();
  });

  test('deleting a trip deletes its cover picture', async () => {
    await newUser();
    const trip = await createTrip(data);
    const cover = await uploadCover(trip.id, png());

    await deleteTrip(trip.id);

    expect((await fetch(imageUrl(cover.id))).status).toBe(404);
  });
});
