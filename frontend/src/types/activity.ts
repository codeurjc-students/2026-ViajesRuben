/** Mirrors the backend ActivityCategory enum. */
export type ActivityCategory =
  'SIGHTSEEING' | 'FOOD' | 'TRANSPORT' | 'ACCOMMODATION' | 'LEISURE' | 'OTHER';

/** Mirrors the backend ActivityDTO. The date is an ISO string (yyyy-MM-dd), the times "HH:mm:ss". */
export interface Activity {
  id: number;
  tripId: number;
  authorId: number;
  name: string;
  description: string | null;
  location: string | null;
  date: string;
  /** Null for an activity without schedule: it lasts the whole day. */
  startTime: string | null;
  endTime: string | null;
  cost: number | null;
  category: ActivityCategory | null;
  /** Null without picture; the picture is at `/api/v1/images/{imageId}`. */
  imageId: string | null;
}

/** Mirrors the backend ActivityRequest: what the form sends to create or replace an activity. Times are "HH:mm". */
export interface ActivityData {
  name: string;
  date: string;
  startTime: string | null;
  endTime: string | null;
  location: string | null;
  description: string | null;
  cost: number | null;
  category: ActivityCategory | null;
}
