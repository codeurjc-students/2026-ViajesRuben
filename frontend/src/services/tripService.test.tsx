import { describe, test, expect, vi, afterEach } from 'vitest';
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
import { pageOf, tripOf, tripsFrom } from '../testHelpers';
import { ApiError, NetworkError } from './apiError';

afterEach(() => {
  vi.restoreAllMocks();
});

test('fetches the first page of trips from the REST API', async () => {
  const page = pageOf(tripsFrom(1, 1));
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({
    ok: true,
    json: () => Promise.resolve(page),
  } as Response);

  const result = await getTrips();

  expect(vi.mocked(fetch).mock.calls[0][0]).toContain('/api/v1/trips?page=0&size=10');
  expect(result).toEqual(page);
});

test('asks for the requested page and size', async () => {
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({
    ok: true,
    json: () => Promise.resolve(pageOf([])),
  } as Response);

  await getTrips(2, 5);

  expect(vi.mocked(fetch).mock.calls[0][0]).toContain('/api/v1/trips?page=2&size=5');
});

test('turns a non-2xx answer into an ApiError with the status', async () => {
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({ ok: false, status: 503 } as Response);

  await expect(getTrips()).rejects.toMatchObject({ name: 'ApiError', status: 503 });
  await expect(getTrips()).rejects.toBeInstanceOf(ApiError);
});

test('turns a failed connection into a NetworkError', async () => {
  vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('Failed to fetch'));

  await expect(getTrips()).rejects.toBeInstanceOf(NetworkError);
});

const data = {
  name: 'Escapada',
  destination: 'Lisboa',
  startDate: '2027-05-01',
  endDate: '2027-05-05',
  budget: 1200.5,
  description: null,
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

describe('B4 trip operations', () => {
  test('getTrip reads one trip', async () => {
    answer(tripOf({ id: 7 }));

    await expect(getTrip(7)).resolves.toMatchObject({ id: 7 });

    expect(lastCall().url).toContain('/api/v1/trips/7');
    expect(lastCall().init).toBeUndefined();
  });

  test('createTrip posts the data as JSON and returns the created trip', async () => {
    answer(tripOf({ id: 8 }), 201);

    await expect(createTrip(data)).resolves.toMatchObject({ id: 8 });

    const { url, init } = lastCall();
    expect(url).toMatch(/\/api\/v1\/trips$/);
    expect(init?.method).toBe('POST');
    expect(JSON.parse(init?.body as string)).toEqual(data);
  });

  test('updateTrip puts the data on the trip', async () => {
    answer(tripOf({ id: 8 }));

    await updateTrip(8, data);

    const { url, init } = lastCall();
    expect(url).toContain('/api/v1/trips/8');
    expect(init?.method).toBe('PUT');
    expect(JSON.parse(init?.body as string)).toEqual(data);
  });

  test('deleteTrip sends DELETE and accepts the empty answer', async () => {
    noContent();

    await expect(deleteTrip(8)).resolves.toBeUndefined();

    expect(lastCall().url).toContain('/api/v1/trips/8');
    expect(lastCall().init?.method).toBe('DELETE');
  });

  test('uploadCover sends the file as multipart in the "file" part', async () => {
    answer({ id: 'img', contentType: 'image/png', size: 4 }, 201);
    const file = new File([new Uint8Array(4)], 'a.png', { type: 'image/png' });

    await expect(uploadCover(8, file)).resolves.toMatchObject({ id: 'img' });

    const { url, init } = lastCall();
    expect(url).toContain('/api/v1/trips/8/cover');
    expect(init?.method).toBe('PUT');
    expect((init?.body as FormData).get('file')).toBeInstanceOf(File);
  });

  test('deleteCover sends DELETE to the cover', async () => {
    noContent();

    await deleteCover(8);

    expect(lastCall().url).toContain('/api/v1/trips/8/cover');
    expect(lastCall().init?.method).toBe('DELETE');
  });

  test('applyCover keeps the cover without calling the server', async () => {
    noContent();

    await applyCover(8, { kind: 'keep' });

    expect(fetch).not.toHaveBeenCalled();
  });

  test('applyCover removes the cover', async () => {
    noContent();

    await applyCover(8, { kind: 'remove' });

    expect(lastCall().init?.method).toBe('DELETE');
  });

  test('applyCover uploads the chosen file', async () => {
    answer({ id: 'img', contentType: 'image/png', size: 1 }, 201);

    await applyCover(8, { kind: 'replace', file: new File(['x'], 'a.png', { type: 'image/png' }) });

    expect(lastCall().url).toContain('/api/v1/trips/8/cover');
    expect(lastCall().init?.method).toBe('PUT');
  });

  test.each([400, 401, 403, 404])(
    'a %i answer becomes an ApiError with that status',
    async (status) => {
      vi.spyOn(globalThis, 'fetch').mockResolvedValue({ ok: false, status } as Response);

      await expect(getTrip(8)).rejects.toMatchObject({ name: 'ApiError', status });
    },
  );
});
