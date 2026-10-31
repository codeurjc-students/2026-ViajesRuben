// @vitest-environment node
import { afterEach, describe, expect, test, vi } from 'vitest';
import { newUser, PASSWORD, sendCookie, sessionCookie, uniqueEmail } from '../integrationHelpers';
import { getCurrentUser, login, register } from './authService';
import { getUsers, setUserStatus, updateUser } from './adminUserService';

// The development administrator (ADMIN_EMAIL / ADMIN_PASSWORD_HASH defaults), as in CI.
const ADMIN_EMAIL = 'admin@tripmate.com';
const ADMIN_PASSWORD = 'admin';

afterEach(() => {
  vi.restoreAllMocks();
});

async function asAdmin() {
  sendCookie(await sessionCookie(ADMIN_EMAIL, ADMIN_PASSWORD));
}

describe('B7 adminUserService against the REST API', () => {
  test('without a session every operation is a 401', async () => {
    await expect(getUsers('')).rejects.toMatchObject({ status: 401 });
    await expect(updateUser(1, { name: 'Ana', email: 'a@b.es' })).rejects.toMatchObject({
      status: 401,
    });
    await expect(setUserStatus(1, 'BLOCKED')).rejects.toMatchObject({ status: 401 });
  });

  test('a regular user gets 403 on every operation', async () => {
    const { user } = await newUser();

    await expect(getUsers('')).rejects.toMatchObject({ status: 403 });
    await expect(updateUser(user.id, { name: 'Ana', email: 'a@b.es' })).rejects.toMatchObject({
      status: 403,
    });
    await expect(setUserStatus(user.id, 'BLOCKED')).rejects.toMatchObject({ status: 403 });
  });

  test('the administrator finds a user by name or email, ignoring case', async () => {
    const email = uniqueEmail();
    // Unique, because the development database keeps the users of earlier runs.
    const name = `Zelda Búsqueda ${Date.now()}`;
    const user = await register({ name, email, password: PASSWORD });
    await asAdmin();

    const byName = await getUsers(name.toLowerCase());
    const byEmail = await getUsers(email.toUpperCase());

    expect(byName.content.map((found) => found.id)).toEqual([user.id]);
    expect(byEmail.content).toEqual([
      {
        id: user.id,
        name,
        email,
        status: 'ACTIVE',
        registrationDate: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/),
      },
    ]);
    expect(byEmail.content[0]).not.toHaveProperty('password');
    expect((await getUsers('no-user-has-this-text-7f3a')).content).toEqual([]);
  });

  test('the list is paginated and never contains the administrator', async () => {
    await register({ name: 'Paginado A', email: uniqueEmail(), password: PASSWORD });
    await register({ name: 'Paginado B', email: uniqueEmail(), password: PASSWORD });
    await asAdmin();

    const first = await getUsers('Paginado', 0, 1);
    const second = await getUsers('Paginado', 1, 1);

    expect(first.content).toHaveLength(1);
    expect(first.last).toBe(false);
    expect(first.totalElements).toBeGreaterThanOrEqual(2);
    expect(second.content).toHaveLength(1);
    expect(second.content[0].id).not.toBe(first.content[0].id);
    expect((await getUsers(ADMIN_EMAIL)).content).toEqual([]);
  });

  test('editing changes the name and email, and a taken email is a 409', async () => {
    const email = uniqueEmail();
    const user = await register({ name: 'Editable', email, password: PASSWORD });
    const other = await register({ name: 'Otro', email: uniqueEmail(), password: PASSWORD });
    await asAdmin();
    const newEmail = uniqueEmail();

    const updated = await updateUser(user.id, { name: 'Editado', email: newEmail });

    expect(updated).toMatchObject({ id: user.id, name: 'Editado', email: newEmail });
    await expect(
      updateUser(user.id, { name: 'Editado', email: other.email }),
    ).rejects.toMatchObject({ status: 409 });
    await expect(updateUser(999999999, { name: 'X', email: uniqueEmail() })).rejects.toMatchObject({
      status: 404,
    });
  });

  test('blocking stops the login and the open session, and unblocking restores them', async () => {
    const email = uniqueEmail();
    const user = await register({ name: 'Bloqueable', email, password: PASSWORD });
    const userCookie = await sessionCookie(email);
    await asAdmin();

    const blocked = await setUserStatus(user.id, 'BLOCKED');

    expect(blocked.status).toBe('BLOCKED');
    await expect(login({ email, password: PASSWORD })).rejects.toMatchObject({ status: 403 });
    vi.restoreAllMocks();
    sendCookie(userCookie);
    await expect(getCurrentUser()).resolves.toBeNull();

    vi.restoreAllMocks();
    await asAdmin();
    expect((await setUserStatus(user.id, 'ACTIVE')).status).toBe('ACTIVE');
    await expect(login({ email, password: PASSWORD })).resolves.toMatchObject({ id: user.id });
  });

  test('the administrator cannot block or edit their own account', async () => {
    await asAdmin();
    const admin = await getCurrentUser();
    expect(admin).not.toBeNull();

    await expect(setUserStatus(admin!.id, 'BLOCKED')).rejects.toMatchObject({ status: 403 });
    await expect(
      updateUser(admin!.id, { name: 'Boss', email: 'boss@example.com' }),
    ).rejects.toMatchObject({ status: 403 });
  });
});
