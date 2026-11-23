/** Mirrors the backend TripDTO. Dates are ISO strings (yyyy-MM-dd). */
export interface Trip {
  id: number;
  creatorId: number;
  name: string;
  destination: string;
  startDate: string;
  endDate: string;
  /** Null when the trip has no budget. */
  budget: number | null;
  description: string | null;
  /** Null without cover; the picture is at `/api/v1/images/{coverImageId}`. */
  coverImageId: string | null;
  /** True when the trip is a public itinerary (I7). */
  published: boolean;
  /** ISO instant of the publication; null while the trip is not published. */
  publishedAt: string | null;
  /** Id of the public trip this one was copied from (I9); null if it is no copy or the original was deleted. */
  copiedFromId: number | null;
  /** How many people joined the trip (I3); the creator is not counted. */
  participantCount: number;
}

/** Mirrors the backend AdminTripDTO: a trip in the administrator's moderation list. */
export interface AdminTrip {
  id: number;
  name: string;
  destination: string;
  startDate: string;
  endDate: string;
  creatorId: number;
  creatorEmail: string;
}

/** Mirrors the backend TripRequest: what the form sends to create or replace a trip. */
export interface TripData {
  name: string;
  destination: string;
  startDate: string;
  endDate: string;
  budget: number | null;
  description: string | null;
}

/** What to do with the cover when a trip is saved. */
export type CoverChange = { kind: 'keep' } | { kind: 'replace'; file: File } | { kind: 'remove' };

/** Mirrors the backend DayPreviewDTO: the first activity of one day of a public trip (`day` counts from 1). */
export interface DayPreview {
  day: number;
  activityName: string;
}

/** Mirrors the backend PublicTripDTO: a public itinerary as anyone sees it; the author's name, never an email. */
export interface PublicTrip {
  id: number;
  name: string;
  destination: string;
  startDate: string;
  endDate: string;
  /** Null when the trip has no budget. */
  budget: number | null;
  description: string | null;
  /** Null without cover; the picture is at `/api/v1/images/{coverImageId}`. */
  coverImageId: string | null;
  authorName: string;
  publishedAt: string;
}

/** Mirrors the backend PublicTripSummaryDTO: a public itinerary of the home carousel with a preview of its first days. */
export interface PublicTripSummary {
  trip: PublicTrip;
  firstDays: DayPreview[];
}
