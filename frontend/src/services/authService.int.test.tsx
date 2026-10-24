import { test, expect, describe } from 'vitest';
import { getCurrentUser, login, register } from './authService';
import { ApiError } from './apiError';

const PASSWORD = 'secret-pass';

function uniqueEmail() {
  return `ana-${Date.now()}-${Math.floor(Math.random() * 1e6)}@example.com`;
}

describe('B2 authService against the REST API', () => {
  test('register creates a regular user and never returns the password', async () => {
    const email = uniqueEmail();

    const user = await register({ name: 'Ana', email, password: PASSWORD });

    expect(user).toMatchObject({ name: 'Ana', email, role: 'USER' });
    expect(user.id).toBeGreaterThan(0);
    expect(user).not.toHaveProperty('password');
  });

  test('a registered user can log in with the chosen password', async () => {
    const email = uniqueEmail();
    await register({ name: 'Ana', email, password: PASSWORD });

    await expect(login({ email, password: PASSWORD })).resolves.toMatchObject({ email });
  });

  test('registering the same email twice is a 409', async () => {
    const email = uniqueEmail();
    await register({ name: 'Ana', email, password: PASSWORD });

    const second = register({ name: 'Other', email: email.toUpperCase(), password: PASSWORD });

    await expect(second).rejects.toBeInstanceOf(ApiError);
    await expect(second).rejects.toMatchObject({ status: 409 });
  });

  test('invalid data is a 400', async () => {
    await expect(
      register({ name: 'Ana', email: 'not-an-email', password: PASSWORD }),
    ).rejects.toMatchObject({ status: 400 });
    await expect(
      register({ name: 'Ana', email: uniqueEmail(), password: 'short' }),
    ).rejects.toMatchObject({ status: 400 });
  });

  test('a wrong password is a 401', async () => {
    const email = uniqueEmail();
    await register({ name: 'Ana', email, password: PASSWORD });

    await expect(login({ email, password: 'other-pass' })).rejects.toMatchObject({ status: 401 });
  });

  test('without a session cookie nobody is logged in', async () => {
    await expect(getCurrentUser()).resolves.toBeNull();
  });
});
