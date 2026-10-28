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

/** Mirrors the backend DayPreviewDTO: the first activity of one day of an example trip (`day` counts from 1). */
export interface DayPreview {
  day: number;
  activityName: string;
}

/** Mirrors the backend ExampleTripDTO: an example trip of the public home page with a preview of its first days. */
export interface ExampleTrip {
  trip: Trip;
  firstDays: DayPreview[];
}
