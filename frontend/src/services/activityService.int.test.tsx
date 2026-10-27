// @vitest-environment node
// Node's fetch needs Node's own FormData and File for the multipart upload, not the jsdom ones.
import { afterEach, describe, expect, test, vi } from 'vitest';
import { newUser, png } from '../integrationHelpers';
import type { ActivityData } from '../types';
import {
  applyActivityImage,
  createActivity,
  deleteActivity,
  deleteActivityImage,
  getActivities,
  getActivity,
  updateActivity,
  uploadActivityImage,
} from './activityService';
import { imageUrl } from './imageService';
import { createTrip, deleteTrip, getTrip, updateTrip } from './tripService';

const tripData = {
  name: 'Roma',
  destination: 'Roma',
  startDate: '2027-07-10',
  endDate: '2027-07-14',
  budget: null,
  description: null,
};

function activityData(overrides: Partial<ActivityData> = {}): ActivityData {
  return {
    name: 'Coliseo',
    date: '2027-07-11',
    startTime: null,
    endTime: null,
    location: null,
    description: null,
    cost: null,
    category: null,
    ...overrides,
  };
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe('B5 activityService against the REST API', () => {
  test('without a session every activity operation is a 401', async () => {
    await expect(getActivities(1)).rejects.toMatchObject({ status: 401 });
    await expect(getActivity(1, 1)).rejects.toMatchObject({ status: 401 });
    await expect(createActivity(1, activityData())).rejects.toMatchObject({ status: 401 });
    await expect(updateActivity(1, 1, activityData())).rejects.toMatchObject({ status: 401 });
    await expect(deleteActivity(1, 1)).rejects.toMatchObject({ status: 401 });
    await expect(uploadActivityImage(1, 1, png())).rejects.toMatchObject({ status: 401 });
    await expect(deleteActivityImage(1, 1)).rejects.toMatchObject({ status: 401 });
  });

  test('creates, reads, updates and deletes an activity', async () => {
    const { user } = await newUser();
    const trip = await createTrip(tripData);

    const created = await createActivity(
      trip.id,
      activityData({ startTime: '09:30', endTime: '11:00', cost: 16.5, category: 'SIGHTSEEING' }),
    );
    expect(created).toMatchObject({
      tripId: trip.id,
      authorId: user.id,
      name: 'Coliseo',
      startTime: '09:30:00',
      endTime: '11:00:00',
      cost: 16.5,
      category: 'SIGHTSEEING',
      imageId: null,
    });
    await expect(getActivity(trip.id, created.id)).resolves.toEqual(created);

    const updated = await updateActivity(
      trip.id,
      created.id,
      activityData({ name: 'Foro', date: '2027-07-12' }),
    );
    expect(updated).toMatchObject({ id: created.id, name: 'Foro', startTime: null, cost: null });

    await expect(deleteActivity(trip.id, created.id)).resolves.toBeUndefined();
    await expect(getActivity(trip.id, created.id)).rejects.toMatchObject({ status: 404 });
  });

  test('lists the itinerary in time order, all of it or one day at a time', async () => {
    await newUser();
    const trip = await createTrip(tripData);
    await createActivity(trip.id, activityData({ name: 'Cena', startTime: '20:00' }));
    await createActivity(trip.id, activityData({ name: 'Museo', startTime: '10:00' }));
    await createActivity(trip.id, activityData({ name: 'Mercado' }));
    await createActivity(trip.id, activityData({ name: 'Llegada', date: '2027-07-10' }));

    const all = await getActivities(trip.id);
    const day = await getActivities(trip.id, '2027-07-11');
    const emptyDay = await getActivities(trip.id, '2027-07-13');

    expect(all.content.map((activity) => activity.name)).toEqual([
      'Llegada',
      'Mercado',
      'Museo',
      'Cena',
    ]);
    expect(day.content.map((activity) => activity.name)).toEqual(['Mercado', 'Museo', 'Cena']);
    expect(day.totalElements).toBe(3);
    expect(emptyDay).toMatchObject({ content: [], totalElements: 0, last: true });
  });

  test('pages through a long day', async () => {
    await newUser();
    const trip = await createTrip(tripData);
    for (const hour of ['08', '09', '10']) {
      await createActivity(trip.id, activityData({ name: `A ${hour}`, startTime: `${hour}:00` }));
    }

    const first = await getActivities(trip.id, '2027-07-11', 0, 2);
    const second = await getActivities(trip.id, '2027-07-11', 1, 2);

    expect(first).toMatchObject({ size: 2, totalElements: 3, last: false });
    expect(second.content.map((activity) => activity.name)).toEqual(['A 10']);
    expect(second.last).toBe(true);
  });

  test('a date outside the trip, a bad time or an empty name is a 400 and nothing is created', async () => {
    await newUser();
    const trip = await createTrip(tripData);

    await expect(
      createActivity(trip.id, activityData({ date: '2027-07-09' })),
    ).rejects.toMatchObject({ status: 400 });
    await expect(
      createActivity(trip.id, activityData({ date: '2027-07-15' })),
    ).rejects.toMatchObject({ status: 400 });
    await expect(
      createActivity(trip.id, activityData({ startTime: '12:00', endTime: '10:00' })),
    ).rejects.toMatchObject({ status: 400 });
    await expect(createActivity(trip.id, activityData({ endTime: '10:00' }))).rejects.toMatchObject(
      { status: 400 },
    );
    await expect(createActivity(trip.id, activityData({ name: ' ' }))).rejects.toMatchObject({
      status: 400,
    });
    await expect(getActivities(trip.id)).resolves.toMatchObject({ totalElements: 0 });
  });

  test('an activity of another trip of the same user is a 404', async () => {
    await newUser();
    const trip = await createTrip(tripData);
    const other = await createTrip(tripData);
    const activity = await createActivity(trip.id, activityData());

    await expect(getActivity(other.id, activity.id)).rejects.toMatchObject({ status: 404 });
    await expect(updateActivity(other.id, activity.id, activityData())).rejects.toMatchObject({
      status: 404,
    });
    await expect(deleteActivity(other.id, activity.id)).rejects.toMatchObject({ status: 404 });
    await expect(getActivity(trip.id, activity.id)).resolves.toMatchObject({ id: activity.id });
  });

  test('another user cannot read, change or delete the activities of a trip: 403', async () => {
    await newUser('Ana');
    const trip = await createTrip(tripData);
    const activity = await createActivity(trip.id, activityData());
    vi.restoreAllMocks();
    await newUser('Bob');

    await expect(getActivities(trip.id)).rejects.toMatchObject({ status: 403 });
    await expect(getActivity(trip.id, activity.id)).rejects.toMatchObject({ status: 403 });
    await expect(createActivity(trip.id, activityData())).rejects.toMatchObject({ status: 403 });
    await expect(updateActivity(trip.id, activity.id, activityData())).rejects.toMatchObject({
      status: 403,
    });
    await expect(deleteActivity(trip.id, activity.id)).rejects.toMatchObject({ status: 403 });
    await expect(uploadActivityImage(trip.id, activity.id, png())).rejects.toMatchObject({
      status: 403,
    });
  });

  test('a trip or an activity that does not exist is a 404', async () => {
    await newUser();
    const trip = await createTrip(tripData);

    await expect(getActivities(999_999_999)).rejects.toMatchObject({ status: 404 });
    await expect(createActivity(999_999_999, activityData())).rejects.toMatchObject({
      status: 404,
    });
    await expect(getActivity(trip.id, 999_999_999)).rejects.toMatchObject({ status: 404 });
    await expect(deleteActivity(trip.id, 999_999_999)).rejects.toMatchObject({ status: 404 });
  });

  test('uploads, replaces and deletes the image, deleting the old pictures', async () => {
    await newUser();
    const trip = await createTrip(tripData);
    const activity = await createActivity(trip.id, activityData());

    const first = await uploadActivityImage(trip.id, activity.id, png());
    expect((await getActivity(trip.id, activity.id)).imageId).toBe(first.id);
    expect((await fetch(imageUrl(first.id))).status).toBe(200);

    const second = await uploadActivityImage(trip.id, activity.id, png());
    expect((await getActivity(trip.id, activity.id)).imageId).toBe(second.id);
    expect((await fetch(imageUrl(first.id))).status).toBe(404);

    await deleteActivityImage(trip.id, activity.id);
    expect((await getActivity(trip.id, activity.id)).imageId).toBeNull();
    expect((await fetch(imageUrl(second.id))).status).toBe(404);
  });

  test('an image that is not a picture is a 400 and keeps the current one', async () => {
    await newUser();
    const trip = await createTrip(tripData);
    const activity = await createActivity(trip.id, activityData());
    const image = await uploadActivityImage(trip.id, activity.id, png());

    await expect(
      uploadActivityImage(
        trip.id,
        activity.id,
        new File(['hello'], 'a.png', { type: 'image/png' }),
      ),
    ).rejects.toMatchObject({ status: 400 });

    expect((await getActivity(trip.id, activity.id)).imageId).toBe(image.id);
  });

  test('applyActivityImage replaces, keeps and removes the image', async () => {
    await newUser();
    const trip = await createTrip(tripData);
    const activity = await createActivity(trip.id, activityData());

    await applyActivityImage(trip.id, activity.id, { kind: 'replace', file: png() });
    expect((await getActivity(trip.id, activity.id)).imageId).not.toBeNull();
    await applyActivityImage(trip.id, activity.id, { kind: 'keep' });
    expect((await getActivity(trip.id, activity.id)).imageId).not.toBeNull();
    await applyActivityImage(trip.id, activity.id, { kind: 'remove' });
    expect((await getActivity(trip.id, activity.id)).imageId).toBeNull();
  });

  test('deleting an activity deletes its picture', async () => {
    await newUser();
    const trip = await createTrip(tripData);
    const activity = await createActivity(trip.id, activityData());
    const image = await uploadActivityImage(trip.id, activity.id, png());

    await deleteActivity(trip.id, activity.id);

    expect((await fetch(imageUrl(image.id))).status).toBe(404);
  });

  test('deleting a trip deletes its activities and their pictures', async () => {
    await newUser();
    const trip = await createTrip(tripData);
    const activity = await createActivity(trip.id, activityData());
    const image = await uploadActivityImage(trip.id, activity.id, png());

    await deleteTrip(trip.id);

    await expect(getActivities(trip.id)).rejects.toMatchObject({ status: 404 });
    expect((await fetch(imageUrl(image.id))).status).toBe(404);
  });

  test('trip dates may leave an activity outside, which still can be read but not moved there', async () => {
    await newUser();
    const trip = await createTrip(tripData);
    const activity = await createActivity(trip.id, activityData({ date: '2027-07-14' }));
    const shorter = { ...tripData, endDate: '2027-07-12' };

    await expect(updateTrip(trip.id, shorter)).resolves.toMatchObject({ endDate: '2027-07-12' });

    expect((await getTrip(trip.id)).endDate).toBe('2027-07-12');
    await expect(getActivity(trip.id, activity.id)).resolves.toMatchObject({ date: '2027-07-14' });
    await expect(
      updateActivity(trip.id, activity.id, activityData({ date: '2027-07-13' })),
    ).rejects.toMatchObject({ status: 400 });
    await expect(
      updateActivity(trip.id, activity.id, activityData({ date: '2027-07-11' })),
    ).resolves.toMatchObject({ date: '2027-07-11' });
  });
});
