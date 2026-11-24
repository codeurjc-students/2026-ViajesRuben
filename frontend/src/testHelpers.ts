import { vi } from 'vitest';
import type { AuthContextValue } from './context/AuthContext';
import type {
  Activity,
  ActivityLogEntry,
  AdminTrip,
  AdminUser,
  Invitation,
  Page,
  Participant,
  PublicTripSummary,
  ReceivedInvitation,
  Trip,
  TripConflicts,
  TripSpending,
  User,
} from './types';

export const ANA: User = {
  id: 1,
  name: 'Ana',
  email: 'ana@example.com',
  role: 'USER',
  avatarId: null,
  registrationDate: '2026-10-01',
};

/** Builds a user as the administrator sees it: Ana, active, unless overridden. */
export function adminUserOf(overrides: Partial<AdminUser> = {}): AdminUser {
  return {
    id: 5,
    name: 'Ana',
    email: 'ana@example.com',
    status: 'ACTIVE',
    registrationDate: '2026-10-01',
    ...overrides,
  };
}

/** Builds a trip as the administrator's moderation list shows it: Escapada by Ana unless overridden. */
export function adminTripOf(overrides: Partial<AdminTrip> = {}): AdminTrip {
  return {
    id: 1,
    name: 'Escapada',
    destination: 'Lisboa',
    startDate: '2027-05-01',
    endDate: '2027-05-05',
    creatorId: 5,
    creatorEmail: 'ana@example.com',
    ...overrides,
  };
}

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

/** Builds a participant of the API for tests: Bob, who joined with no avatar and no email unless overridden. */
export function participantOf(overrides: Partial<Participant> = {}): Participant {
  return {
    userId: 2,
    name: 'Bob',
    avatarId: null,
    email: null,
    joinedAt: '2026-11-18T10:00:00Z',
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
    published: false,
    publishedAt: null,
    copiedFromId: null,
    participantCount: 0,
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
    authorName: 'Ana',
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

/** Builds a line of the trip's change log for tests: Bob added "Museo" a moment ago. */
export function logEntryOf(overrides: Partial<ActivityLogEntry> = {}): ActivityLogEntry {
  return {
    id: 1,
    activityId: 1,
    activityName: 'Museo',
    userId: 2,
    userName: 'Bob',
    action: 'CREATED',
    changedFields: [],
    createdAt: new Date().toISOString(),
    ...overrides,
  };
}

/** Builds a public itinerary of the home page for tests: 5 days in Roma by Carlos, with a preview of the first two. */
export function publicTripOf(overrides: Partial<PublicTripSummary> = {}): PublicTripSummary {
  return {
    trip: {
      id: 10,
      name: 'Roma en cinco días',
      destination: 'Roma',
      startDate: '2027-05-10',
      endDate: '2027-05-14',
      budget: 1200,
      description: null,
      coverImageId: null,
      authorName: 'Carlos',
      publishedAt: '2026-11-01T10:00:00Z',
    },
    firstDays: [
      { day: 1, activityName: 'Coliseo' },
      { day: 2, activityName: 'Vaticano' },
    ],
    ...overrides,
  };
}

/** Builds a pending invitation of the API for tests. */
export function invitationOf(overrides: Partial<Invitation> = {}): Invitation {
  return {
    id: 1,
    invitedEmail: 'friend@example.com',
    status: 'PENDING',
    sentAt: '2026-11-18T10:15:30Z',
    respondedAt: null,
    ...overrides,
  };
}

/** Builds a pending invitation as its addressee sees it, for tests. */
export function receivedInvitationOf(
  overrides: Partial<ReceivedInvitation> = {},
): ReceivedInvitation {
  return {
    id: 1,
    tripId: 5,
    tripName: 'Verano en Roma',
    inviterName: 'Ana',
    status: 'PENDING',
    sentAt: '2026-11-18T10:15:30Z',
    respondedAt: null,
    ...overrides,
  };
}

/** Builds the spending charts' data for tests: no budget and no cost unless overridden. */
export function spendingOf(overrides: Partial<TripSpending> = {}): TripSpending {
  return { budget: null, totalCost: 0, byCategory: [], byDay: [], ...overrides };
}

/** Builds the conflicts of a trip for tests: none unless overridden. */
export function conflictsOf(overrides: Partial<TripConflicts> = {}): TripConflicts {
  return { overlaps: [], outsideDates: [], budget: null, ...overrides };
}
