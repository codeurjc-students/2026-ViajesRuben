import { afterEach, describe, expect, test, vi } from 'vitest';
import { getUsers, setUserStatus, updateUser } from './adminUserService';
import { ApiError } from './apiError';
import { adminUserOf, pageOf } from '../testHelpers';

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

describe('B7 adminUserService', () => {
  test('getUsers asks for a page without a search when the text is blank', async () => {
    answer(200, pageOf([adminUserOf()]));

    await getUsers('   ', 2, 5);

    expect(vi.mocked(fetch).mock.calls[0][0]).toContain('/api/v1/admin/users?page=2&size=5');
    expect(vi.mocked(fetch).mock.calls[0][0]).not.toContain('search');
  });

  test('getUsers sends the trimmed search text, encoded', async () => {
    answer(200, pageOf([adminUserOf()]));

    await expect(getUsers(' ana ruiz&x ')).resolves.toEqual(pageOf([adminUserOf()]));

    expect(vi.mocked(fetch).mock.calls[0][0]).toContain('search=ana+ruiz%26x');
    expect(vi.mocked(fetch).mock.calls[0][0]).toContain('page=0&size=10');
  });

  test('getUsers turns a non-administrator into an ApiError 403', async () => {
    answer(403);

    await expect(getUsers('')).rejects.toBeInstanceOf(ApiError);
    await expect(getUsers('')).rejects.toMatchObject({ status: 403 });
  });

  test('updateUser puts name and email as JSON and returns the user', async () => {
    answer(200, adminUserOf({ name: 'Ana Ruiz' }));

    await expect(updateUser(5, { name: 'Ana Ruiz', email: 'ana@example.com' })).resolves.toEqual(
      adminUserOf({ name: 'Ana Ruiz' }),
    );

    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(url).toContain('/api/v1/admin/users/5');
    expect(init).toMatchObject({
      method: 'PUT',
      body: JSON.stringify({ name: 'Ana Ruiz', email: 'ana@example.com' }),
    });
  });

  test('updateUser turns a taken email into an ApiError 409', async () => {
    answer(409);

    await expect(updateUser(5, { name: 'Ana', email: 'bob@example.com' })).rejects.toMatchObject({
      status: 409,
    });
  });

  test('setUserStatus puts the status to the status sub-resource', async () => {
    answer(200, adminUserOf({ status: 'BLOCKED' }));

    await expect(setUserStatus(5, 'BLOCKED')).resolves.toEqual(adminUserOf({ status: 'BLOCKED' }));

    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(url).toContain('/api/v1/admin/users/5/status');
    expect(init).toMatchObject({ method: 'PUT', body: JSON.stringify({ status: 'BLOCKED' }) });
  });
});
