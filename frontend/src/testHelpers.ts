import { vi } from 'vitest';
import type { AuthContextValue } from './context/AuthContext';
import type { Page, Trip, User } from './types';

export const ANA: User = {
  id: 1,
  name: 'Ana',
  email: 'ana@example.com',
  role: 'USER',
  avatarId: null,
  registrationDate: '2026-10-01',
};

/** Builds the value of the auth context for tests: logged in as `user` (or nobody) with spy actions. */
export function authValue(
  user: User | null = ANA,
  overrides: Partial<AuthContextValue> = {},
): AuthContextValue {
  return {
    user,
    loading: false,
    login: vi.fn().mockResolvedValue(undefined),
    logout: vi.fn().mockResolvedValue(undefined),
    register: vi.fn().mockResolvedValue(undefined),
    updateUser: vi.fn(),
    ...overrides,
  };
}

/** Builds a page of the API for tests. */
export function pageOf<T>(content: T[], overrides: Partial<Page<T>> = {}): Page<T> {
  return {
    content,
    page: 0,
    size: 10,
    totalElements: content.length,
    last: true,
    ...overrides,
  };
}

/** Builds `count` trips whose ids start at `firstId`. */
export function tripsFrom(firstId: number, count: number): Trip[] {
  return Array.from({ length: count }, (_, i) => ({
    id: firstId + i,
    name: `Trip ${firstId + i}`,
    destination: `City ${firstId + i}`,
    startDate: '2027-05-01',
    endDate: '2027-05-05',
  }));
}
