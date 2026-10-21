/** Mirrors the backend TripDTO. Dates are ISO strings (yyyy-MM-dd). */
export interface Trip {
  id: number;
  name: string;
  destination: string;
  startDate: string;
  endDate: string;
  /** Not in the backend yet (B4, I3); the UI falls back to sample data when missing. */
  coverImage?: string;
  participantCount?: number;
  isCreator?: boolean;
}
