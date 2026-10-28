import { vi } from 'vitest';
import type { AuthContextValue } from './context/AuthContext';
import type { Activity, ExampleTrip, Page, Trip, TripConflicts, User } from './types';

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

/** Builds a trip of the API for tests; Ana (id 1) is the creator unless overridden. */
export function tripOf(overrides: Partial<Trip> = {}): Trip {
  return {
    id: 1,
    creatorId: 1,
    name: 'Escapada',
    destination: 'Lisboa',
    startDate: '2027-05-01',
    endDate: '2027-05-05',
    budget: null,
    description: null,
    coverImageId: null,
    ...overrides,
  };
}

/** Builds `count` trips whose ids start at `firstId`. */
export function tripsFrom(firstId: number, count: number): Trip[] {
  return Array.from({ length: count }, (_, i) =>
    tripOf({
      id: firstId + i,
      name: `Trip ${firstId + i}`,
      destination: `City ${firstId + i}`,
    }),
  );
}

/** Builds an activity of the API for tests: day 2 of `tripOf()`, created by Ana (id 1). */
export function activityOf(overrides: Partial<Activity> = {}): Activity {
  return {
    id: 1,
    tripId: 1,
    authorId: 1,
    name: 'Torre de Belém',
    description: null,
    location: null,
    date: '2027-05-02',
    startTime: null,
    endTime: null,
    cost: null,
    category: null,
    imageId: null,
    ...overrides,
  };
}

/** Builds an example trip of the home page for tests: 5 days in Roma with a preview of the first two. */
export function exampleOf(overrides: Partial<ExampleTrip> = {}): ExampleTrip {
  return {
    trip: tripOf({
      id: 10,
      creatorId: 99,
      name: 'Roma en cinco días',
      destination: 'Roma',
      startDate: '2027-05-10',
      endDate: '2027-05-14',
      budget: 1200,
    }),
    firstDays: [
      { day: 1, activityName: 'Coliseo' },
      { day: 2, activityName: 'Vaticano' },
    ],
    ...overrides,
  };
}

/** Builds the conflicts of a trip for tests: none unless overridden. */
export function conflictsOf(overrides: Partial<TripConflicts> = {}): TripConflicts {
  return { overlaps: [], outsideDates: [], budget: null, ...overrides };
}
