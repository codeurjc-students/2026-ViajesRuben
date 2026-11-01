import type { AdminTrip, Page } from '../types';
import { request } from './apiError';
import { DEFAULT_PAGE_SIZE } from './tripService';

const TRIPS_URL = '/api/v1/admin/trips';

/**
 * One page of the trips of every user whose name, destination or creator's email contains
 * `search` (all of them when it is blank), newest first; answers 401 without a session and 403 for
 * anyone but the administrator. A trip is deleted with `deleteTrip` of the trip service.
 */
export function getTrips(
  search: string,
  page = 0,
  size = DEFAULT_PAGE_SIZE,
): Promise<Page<AdminTrip>> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (search.trim()) params.set('search', search.trim());
  return request<Page<AdminTrip>>(`${TRIPS_URL}?${params}`);
}
