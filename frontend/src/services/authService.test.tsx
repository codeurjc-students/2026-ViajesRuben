import { test, expect, vi, afterEach, describe } from 'vitest';
import { getCurrentUser, login, logout, register } from './authService';
import { ApiError } from './apiError';

const ANA = {
  id: 1,
  name: 'Ana',
  email: 'ana@example.com',
  role: 'USER',
  avatarId: null,
  registrationDate: '2026-10-01',
};

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

describe('B2 authService', () => {
  test('getCurrentUser returns the user of the session', async () => {
    answer(200, ANA);

    await expect(getCurrentUser()).resolves.toEqual(ANA);
    expect(vi.mocked(fetch).mock.calls[0][0]).toContain('/api/v1/auth/me');
  });

  test('getCurrentUser returns null when nobody is logged in', async () => {
    answer(401);

    await expect(getCurrentUser()).resolves.toBeNull();
  });

  test('getCurrentUser rethrows any other failure', async () => {
    answer(500);

    await expect(getCurrentUser()).rejects.toMatchObject({ name: 'ApiError', status: 500 });
  });

  test('login posts the credentials as JSON and returns the user', async () => {
    answer(200, ANA);

    const user = await login({ email: 'ana@example.com', password: 'secret-pass' });

    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(url).toContain('/api/v1/auth/login');
    expect(init?.method).toBe('POST');
    expect(init?.body).toBe(JSON.stringify({ email: 'ana@example.com', password: 'secret-pass' }));
    expect(user).toEqual(ANA);
  });

  test('login turns a wrong password into an ApiError 401', async () => {
    answer(401);

    await expect(login({ email: 'a@b.es', password: 'x' })).rejects.toBeInstanceOf(ApiError);
    await expect(login({ email: 'a@b.es', password: 'x' })).rejects.toMatchObject({ status: 401 });
  });

  test('logout posts to the API and expects no body', async () => {
    answer(204);

    await expect(logout()).resolves.toBeUndefined();
    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(url).toContain('/api/v1/auth/logout');
    expect(init?.method).toBe('POST');
  });

  test('register posts the data to /api/v1/users and returns the user', async () => {
    answer(201, ANA);

    const user = await register({ name: 'Ana', email: 'ana@example.com', password: 'secret-pass' });

    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(url).toContain('/api/v1/users');
    expect(init?.body).toBe(
      JSON.stringify({ name: 'Ana', email: 'ana@example.com', password: 'secret-pass' }),
    );
    expect(user).toEqual(ANA);
  });

  test('register turns a duplicate email into an ApiError 409', async () => {
    answer(409);

    await expect(
      register({ name: 'Ana', email: 'a@b.es', password: 'secret-pass' }),
    ).rejects.toMatchObject({
      status: 409,
    });
  });
});
