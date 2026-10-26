import { afterEach, describe, expect, test, vi } from 'vitest';
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
import { activityOf, pageOf } from '../testHelpers';

afterEach(() => {
  vi.restoreAllMocks();
});

const data = {
  name: 'Museo',
  date: '2027-05-02',
  startTime: '10:00',
  endTime: null,
  location: null,
  description: null,
  cost: 12.5,
  category: 'SIGHTSEEING' as const,
};

function answer(body: unknown, status = 200) {
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({
    ok: true,
    status,
    json: () => Promise.resolve(body),
  } as Response);
}

function noContent() {
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({ ok: true, status: 204 } as Response);
}

function lastCall() {
  const [url, init] = vi.mocked(fetch).mock.calls[0];
  return { url: String(url), init: init as RequestInit | undefined };
}

describe('B5 activity operations', () => {
  test('getActivities asks for the first page of the whole itinerary', async () => {
    const page = pageOf([activityOf()]);
    answer(page);

    await expect(getActivities(3)).resolves.toEqual(page);

    expect(lastCall().url).toContain('/api/v1/trips/3/activities?page=0&size=10');
    expect(lastCall().url).not.toContain('date=');
  });

  test('getActivities filters by day and asks for the requested page and size', async () => {
    answer(pageOf([]));

    await getActivities(3, '2027-05-02', 2, 5);

    expect(lastCall().url).toContain('/api/v1/trips/3/activities?page=2&size=5&date=2027-05-02');
  });

  test('getActivity reads one activity of the trip', async () => {
    answer(activityOf({ id: 7 }));

    await expect(getActivity(3, 7)).resolves.toMatchObject({ id: 7 });

    expect(lastCall().url).toContain('/api/v1/trips/3/activities/7');
    expect(lastCall().init).toBeUndefined();
  });

  test('createActivity posts the data as JSON on the trip', async () => {
    answer(activityOf({ id: 8 }), 201);

    await expect(createActivity(3, data)).resolves.toMatchObject({ id: 8 });

    const { url, init } = lastCall();
    expect(url).toMatch(/\/api\/v1\/trips\/3\/activities$/);
    expect(init?.method).toBe('POST');
    expect(JSON.parse(init?.body as string)).toEqual(data);
  });

  test('updateActivity puts the data on the activity', async () => {
    answer(activityOf({ id: 8 }));

    await updateActivity(3, 8, data);

    const { url, init } = lastCall();
    expect(url).toContain('/api/v1/trips/3/activities/8');
    expect(init?.method).toBe('PUT');
    expect(JSON.parse(init?.body as string)).toEqual(data);
  });

  test('deleteActivity sends DELETE and accepts the empty answer', async () => {
    noContent();

    await expect(deleteActivity(3, 8)).resolves.toBeUndefined();

    expect(lastCall().url).toContain('/api/v1/trips/3/activities/8');
    expect(lastCall().init?.method).toBe('DELETE');
  });

  test('uploadActivityImage sends the file as multipart in the "file" part', async () => {
    answer({ id: 'img', contentType: 'image/png', size: 4 }, 201);
    const file = new File([new Uint8Array(4)], 'a.png', { type: 'image/png' });

    await expect(uploadActivityImage(3, 8, file)).resolves.toMatchObject({ id: 'img' });

    const { url, init } = lastCall();
    expect(url).toContain('/api/v1/trips/3/activities/8/image');
    expect(init?.method).toBe('PUT');
    expect((init?.body as FormData).get('file')).toBeInstanceOf(File);
  });

  test('deleteActivityImage sends DELETE to the image', async () => {
    noContent();

    await deleteActivityImage(3, 8);

    expect(lastCall().url).toContain('/api/v1/trips/3/activities/8/image');
    expect(lastCall().init?.method).toBe('DELETE');
  });

  test('applyActivityImage keeps the image without calling the server', async () => {
    noContent();

    await applyActivityImage(3, 8, { kind: 'keep' });

    expect(fetch).not.toHaveBeenCalled();
  });

  test('applyActivityImage removes the image', async () => {
    noContent();

    await applyActivityImage(3, 8, { kind: 'remove' });

    expect(lastCall().init?.method).toBe('DELETE');
  });

  test('applyActivityImage uploads the chosen file', async () => {
    answer({ id: 'img', contentType: 'image/png', size: 1 }, 201);

    await applyActivityImage(3, 8, {
      kind: 'replace',
      file: new File(['x'], 'a.png', { type: 'image/png' }),
    });

    expect(lastCall().url).toContain('/api/v1/trips/3/activities/8/image');
    expect(lastCall().init?.method).toBe('PUT');
  });

  test.each([400, 401, 403, 404])(
    'a %i answer becomes an ApiError with that status',
    async (status) => {
      vi.spyOn(globalThis, 'fetch').mockResolvedValue({ ok: false, status } as Response);

      await expect(getActivity(3, 8)).rejects.toMatchObject({ name: 'ApiError', status });
    },
  );
});
