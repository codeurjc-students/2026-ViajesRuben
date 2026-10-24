import type { Credentials, RegisterData, User } from '../types';
import { ApiError, request, sendJson } from './apiError';

function postJson<T>(path: string, body: object): Promise<T> {
  return sendJson<T>('POST', path, body);
}

/** The user of the session cookie, or null when nobody is logged in. */
export async function getCurrentUser(): Promise<User | null> {
  try {
    return await request<User>('/api/v1/auth/me');
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) return null;
    throw error;
  }
}

/** Answers 401 for a wrong email or password and 403 for a blocked account. */
export function login(credentials: Credentials): Promise<User> {
  return postJson<User>('/api/v1/auth/login', credentials);
}

export function logout(): Promise<void> {
  return request<void>('/api/v1/auth/logout', { method: 'POST' });
}

/** Creates the account without logging in; answers 409 when the email is already registered. */
export function register(data: RegisterData): Promise<User> {
  return postJson<User>('/api/v1/users', data);
}
