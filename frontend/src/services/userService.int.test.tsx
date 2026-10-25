// @vitest-environment node
// Node's fetch needs Node's own FormData and File for the multipart upload, not the jsdom ones.
import { afterEach, describe, expect, test, vi } from 'vitest';
import { newUser, PASSWORD, png, uniqueEmail } from '../integrationHelpers';
import { getCurrentUser, login, register } from './authService';
import { imageUrl } from './imageService';
import {
  changePassword,
  deleteAvatar,
  getProfile,
  updateProfile,
  uploadAvatar,
} from './userService';

afterEach(() => {
  vi.restoreAllMocks();
});

describe('B3 userService against the REST API', () => {
  test('without a session every profile operation is a 401', async () => {
    await expect(getProfile(1)).rejects.toMatchObject({ status: 401 });
    await expect(updateProfile(1, { name: 'Ana', email: 'a@b.es' })).rejects.toMatchObject({
      status: 401,
    });
    await expect(
      changePassword(1, { currentPassword: PASSWORD, newPassword: 'other-pass' }),
    ).rejects.toMatchObject({ status: 401 });
    await expect(uploadAvatar(1, png())).rejects.toMatchObject({ status: 401 });
    await expect(deleteAvatar(1)).rejects.toMatchObject({ status: 401 });
  });

  test('a user reads their own profile, without the password', async () => {
    const { user, email } = await newUser();

    const profile = await getProfile(user.id);

    expect(profile).toMatchObject({
      id: user.id,
      name: 'Ana',
      email,
      role: 'USER',
      avatarId: null,
    });
    expect(profile.registrationDate).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(profile).not.toHaveProperty('password');
  });

  test('another user`s profile is a 403 and a missing one a 404', async () => {
    const other = await register({ name: 'Bob', email: uniqueEmail(), password: PASSWORD });
    await newUser();

    await expect(getProfile(other.id)).rejects.toMatchObject({ status: 403 });
    await expect(updateProfile(other.id, { name: 'X', email: 'x@y.es' })).rejects.toMatchObject({
      status: 403,
    });
    await expect(uploadAvatar(other.id, png())).rejects.toMatchObject({ status: 403 });
    await expect(getProfile(2_000_000_000)).rejects.toMatchObject({ status: 404 });
  });

  test('changing name and email keeps the session open with the new email', async () => {
    const { user } = await newUser();
    const newEmail = uniqueEmail().toUpperCase();

    const updated = await updateProfile(user.id, { name: 'Ana Ruiz', email: newEmail });

    expect(updated).toMatchObject({ name: 'Ana Ruiz', email: newEmail.toLowerCase() });
    await expect(getCurrentUser()).resolves.toMatchObject({
      name: 'Ana Ruiz',
      email: newEmail.toLowerCase(),
    });
  });

  test('an email of another account is a 409 and an invalid one a 400', async () => {
    const takenEmail = uniqueEmail();
    await register({ name: 'Bob', email: takenEmail, password: PASSWORD });
    const { user } = await newUser();

    await expect(
      updateProfile(user.id, { name: 'Ana', email: takenEmail.toUpperCase() }),
    ).rejects.toMatchObject({ status: 409 });
    await expect(
      updateProfile(user.id, { name: 'Ana', email: 'not-an-email' }),
    ).rejects.toMatchObject({ status: 400 });
    await expect(updateProfile(user.id, { name: ' ', email: uniqueEmail() })).rejects.toMatchObject(
      { status: 400 },
    );
  });

  test('a wrong current password is a 400 and the old one keeps working', async () => {
    const { user, email } = await newUser();

    await expect(
      changePassword(user.id, { currentPassword: 'wrong-pass', newPassword: 'brand-new-pass' }),
    ).rejects.toMatchObject({ status: 400 });

    await expect(login({ email, password: PASSWORD })).resolves.toMatchObject({ email });
  });

  test('a new password of the wrong length is a 400', async () => {
    const { user } = await newUser();

    await expect(
      changePassword(user.id, { currentPassword: PASSWORD, newPassword: 'short' }),
    ).rejects.toMatchObject({ status: 400 });
  });

  test('the new password logs in and the old one no longer does', async () => {
    const { user, email } = await newUser();

    await expect(
      changePassword(user.id, { currentPassword: PASSWORD, newPassword: 'brand-new-pass' }),
    ).resolves.toBeUndefined();

    await expect(login({ email, password: 'brand-new-pass' })).resolves.toMatchObject({ email });
    await expect(login({ email, password: PASSWORD })).rejects.toMatchObject({ status: 401 });
  });

  test('uploads an avatar, serves it, replaces it and deletes it', async () => {
    const { user } = await newUser();

    const first = await uploadAvatar(user.id, png());
    expect(first.contentType).toBe('image/png');
    expect((await getProfile(user.id)).avatarId).toBe(first.id);
    const firstResponse = await fetch(imageUrl(first.id));
    expect(firstResponse.status).toBe(200);
    expect(firstResponse.headers.get('content-type')).toBe('image/png');

    const second = await uploadAvatar(user.id, png());
    expect(second.id).not.toBe(first.id);
    expect((await getProfile(user.id)).avatarId).toBe(second.id);
    expect((await fetch(imageUrl(first.id))).status).toBe(404);

    await expect(deleteAvatar(user.id)).resolves.toBeUndefined();
    expect((await getProfile(user.id)).avatarId).toBeNull();
    expect((await fetch(imageUrl(second.id))).status).toBe(404);
  });

  test('an avatar that is not an image is a 400', async () => {
    const { user } = await newUser();
    const fake = new File(['this is not an image'], 'a.png', { type: 'image/png' });

    await expect(uploadAvatar(user.id, fake)).rejects.toMatchObject({ status: 400 });
    expect((await getProfile(user.id)).avatarId).toBeNull();
  });
});
