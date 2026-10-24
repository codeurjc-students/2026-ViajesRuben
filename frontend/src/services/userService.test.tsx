import { afterEach, describe, expect, test, vi } from 'vitest';
import { ApiError } from './apiError';
import {
  changePassword,
  deleteAvatar,
  getProfile,
  updateProfile,
  uploadAvatar,
} from './userService';
import { ANA } from '../testHelpers';

function answer(status: number, body?: unknown) {
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  } as Response);
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe('B3 userService', () => {
  test('getProfile reads /api/v1/users/{id}', async () => {
    answer(200, ANA);

    await expect(getProfile(1)).resolves.toEqual(ANA);
    expect(vi.mocked(fetch).mock.calls[0][0]).toContain('/api/v1/users/1');
  });

  test('getProfile turns another user`s profile into an ApiError 403', async () => {
    answer(403);

    await expect(getProfile(2)).rejects.toBeInstanceOf(ApiError);
    await expect(getProfile(2)).rejects.toMatchObject({ status: 403 });
  });

  test('updateProfile puts name and email as JSON and returns the user', async () => {
    answer(200, { ...ANA, name: 'Ana Ruiz' });

    const user = await updateProfile(1, { name: 'Ana Ruiz', email: 'ana@example.com' });

    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(url).toContain('/api/v1/users/1');
    expect(init?.method).toBe('PUT');
    expect(init?.body).toBe(JSON.stringify({ name: 'Ana Ruiz', email: 'ana@example.com' }));
    expect(user.name).toBe('Ana Ruiz');
  });

  test('updateProfile turns a taken email into an ApiError 409', async () => {
    answer(409);

    await expect(updateProfile(1, { name: 'Ana', email: 'b@b.es' })).rejects.toMatchObject({
      status: 409,
    });
  });

  test('changePassword puts both passwords to the password sub-resource and expects no body', async () => {
    answer(204);

    await expect(
      changePassword(1, { currentPassword: 'old-secret', newPassword: 'new-secret' }),
    ).resolves.toBeUndefined();

    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(url).toContain('/api/v1/users/1/password');
    expect(init?.method).toBe('PUT');
    expect(init?.body).toBe(
      JSON.stringify({ currentPassword: 'old-secret', newPassword: 'new-secret' }),
    );
  });

  test('changePassword turns a wrong current password into an ApiError 400', async () => {
    answer(400);

    await expect(
      changePassword(1, { currentPassword: 'x', newPassword: 'new-secret' }),
    ).rejects.toMatchObject({ status: 400 });
  });

  test('uploadAvatar puts the file in the `file` part of a multipart body', async () => {
    const stored = { id: 'abc', contentType: 'image/png', size: 4 };
    answer(201, stored);
    const file = new File([new Uint8Array(4)], 'a.png', { type: 'image/png' });

    await expect(uploadAvatar(1, file)).resolves.toEqual(stored);

    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(url).toContain('/api/v1/users/1/avatar');
    expect(init?.method).toBe('PUT');
    expect((init?.body as FormData).get('file')).toBe(file);
  });

  test('uploadAvatar turns a rejected image into an ApiError', async () => {
    answer(413);

    const file = new File([new Uint8Array(4)], 'a.png', { type: 'image/png' });
    await expect(uploadAvatar(1, file)).rejects.toMatchObject({ status: 413 });
  });

  test('deleteAvatar sends DELETE and expects no body', async () => {
    answer(204);

    await expect(deleteAvatar(1)).resolves.toBeUndefined();
    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(url).toContain('/api/v1/users/1/avatar');
    expect(init?.method).toBe('DELETE');
  });
});
