import { afterEach, describe, expect, test, vi } from 'vitest';
import { pageOf, participantOf } from '../testHelpers';
import { getParticipants, removeParticipant } from './participantService';

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

describe('I3 participantService', () => {
  test('getParticipants asks for the first page of the trip by default', async () => {
    const page = pageOf([participantOf()]);
    answer(page);

    await expect(getParticipants(3)).resolves.toEqual(page);

    expect(vi.mocked(fetch).mock.calls[0][0]).toContain(
      '/api/v1/trips/3/participants?page=0&size=10',
    );
  });

  test('getParticipants asks for the requested page and size', async () => {
    answer(pageOf([]));

    await getParticipants(3, 2, 5);

    expect(vi.mocked(fetch).mock.calls[0][0]).toContain(
      '/api/v1/trips/3/participants?page=2&size=5',
    );
  });

  test('keeps the avatar id as the UUID string the server sent', async () => {
    const avatarId = '3f2c9d1e-7a54-4b8e-9c10-5d6e7f8a9b0c';
    answer(pageOf([participantOf({ avatarId })]));

    const page = await getParticipants(3);

    expect(page.content[0].avatarId).toBe(avatarId);
    expect(typeof page.content[0].avatarId).toBe('string');
  });

  test('removeParticipant deletes the participant of the trip and returns nothing', async () => {
    answer(undefined, 204);

    await expect(removeParticipant(3, 8)).resolves.toBeUndefined();

    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(String(url)).toContain('/api/v1/trips/3/participants/8');
    expect(init?.method).toBe('DELETE');
  });

  test.each([401, 403, 404, 409])(
    'a %i of the server becomes an ApiError with its status',
    async (status) => {
      vi.spyOn(globalThis, 'fetch').mockResolvedValue({ ok: false, status } as Response);

      await expect(getParticipants(3)).rejects.toMatchObject({ status });
      await expect(removeParticipant(3, 8)).rejects.toMatchObject({ status });
    },
  );
});
