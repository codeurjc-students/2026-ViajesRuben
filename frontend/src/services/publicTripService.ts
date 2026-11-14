import type { Activity, Page, PublicTrip, PublicTripSummary } from '../types';
import { request } from './apiError';
import { DEFAULT_PAGE_SIZE } from './tripService';

/** How many public trips the home carousel shows at a time. */
export const PUBLIC_PAGE_SIZE = 3;

/** The carousel stops after this many pages, so it shows the 9 most recently published trips. */
export const CAROUSEL_MAX_PAGES = 3;

const PUBLIC_URL = '/api/v1/trips/public';

/** One page of the public trips (the published ones, newest first) with a preview of their first days; public, no session needed. */
export function getPublicTrips(
  page = 0,
  size = PUBLIC_PAGE_SIZE,
): Promise<Page<PublicTripSummary>> {
  return request<Page<PublicTripSummary>>(`${PUBLIC_URL}?page=${page}&size=${size}`);
}

/** Answers 404 when the trip does not exist or is not published. */
export function getPublicTrip(id: number): Promise<PublicTrip> {
  return request<PublicTrip>(`${PUBLIC_URL}/${id}`);
}

/** Like `getActivities` for a public trip: one page in time order, only those of `date` (yyyy-MM-dd) when given; 404 as above. */
export function getPublicActivities(
  tripId: number,
  date?: string,
  page = 0,
  size = DEFAULT_PAGE_SIZE,
): Promise<Page<Activity>> {
  const day = date ? `&date=${date}` : '';
  return request<Page<Activity>>(
    `${PUBLIC_URL}/${tripId}/activities?page=${page}&size=${size}${day}`,
  );
}
