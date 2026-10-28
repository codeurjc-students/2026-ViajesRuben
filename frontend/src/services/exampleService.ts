import type { Activity, ExampleTrip, Page, Trip } from '../types';
import { request } from './apiError';
import { DEFAULT_PAGE_SIZE } from './tripService';

/** How many example trips the home page shows at a time. */
export const EXAMPLES_PAGE_SIZE = 3;

const EXAMPLES_URL = '/api/v1/trips/examples';

/** One page of the example trips (the administrator's) with a preview of their first days; public, no session needed. */
export function getExampleTrips(page = 0, size = EXAMPLES_PAGE_SIZE): Promise<Page<ExampleTrip>> {
  return request<Page<ExampleTrip>>(`${EXAMPLES_URL}?page=${page}&size=${size}`);
}

/** Answers 404 when the trip does not exist or is not an example trip. */
export function getExampleTrip(id: number): Promise<Trip> {
  return request<Trip>(`${EXAMPLES_URL}/${id}`);
}

/** Like `getActivities` for an example trip: one page in time order, only those of `date` (yyyy-MM-dd) when given; 404 as above. */
export function getExampleActivities(
  tripId: number,
  date?: string,
  page = 0,
  size = DEFAULT_PAGE_SIZE,
): Promise<Page<Activity>> {
  const day = date ? `&date=${date}` : '';
  return request<Page<Activity>>(
    `${EXAMPLES_URL}/${tripId}/activities?page=${page}&size=${size}${day}`,
  );
}
