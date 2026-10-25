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
  /** Provisional (see utils/sampleTripDetails): removed with the trip card of B4. */
  participantCount?: number;
  isCreator?: boolean;
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
