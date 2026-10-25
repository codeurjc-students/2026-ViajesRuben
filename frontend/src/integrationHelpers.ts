import { expect, vi } from 'vitest';
import { API_URL } from './services/apiError';
import { register } from './services/authService';

/** Used by every user these helpers create. */
export const PASSWORD = 'secret-pass';

const PNG_SIGNATURE = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a];

export function uniqueEmail() {
  return `ana-${Date.now()}-${Math.floor(Math.random() * 1e6)}@example.com`;
}

/** A tiny file the backend accepts as a PNG (it checks the signature, not the whole image). */
export function png() {
  return new File([new Uint8Array([...PNG_SIGNATURE, 1, 2, 3, 4])], 'a.png', {
    type: 'image/png',
  });
}

/** Logs in with plain fetch and returns the `AuthToken=...` pair of the Set-Cookie header. */
export async function sessionCookie(email: string, password = PASSWORD): Promise<string> {
  const response = await fetch(`${API_URL}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  expect(response.status).toBe(200);
  return response.headers.getSetCookie()[0].split(';')[0];
}

/** Node keeps no cookies, so every service call sends the given session cookie by hand. */
export function sendCookie(cookie: string) {
  const realFetch = globalThis.fetch;
  vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) =>
    realFetch(input, { ...init, headers: { ...init?.headers, Cookie: cookie } }),
  );
}

/** Registers a new user and starts sending its session cookie. */
export async function newUser(name = 'Ana') {
  const email = uniqueEmail();
  const user = await register({ name, email, password: PASSWORD });
  const cookie = await sessionCookie(email);
  sendCookie(cookie);
  return { user, email, cookie };
}
