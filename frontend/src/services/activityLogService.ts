import type { ActivityLogEntry, Page } from '../types';
import { request } from './apiError';
import { DEFAULT_PAGE_SIZE } from './tripService';

/**
 * One page of the trip's change log (I4), newest first: who added, edited or deleted which
 * activity, and when. Anyone who can see the trip may ask: answers 403 for anyone else and 404
 * when the trip does not exist.
 */
export function getActivityLog(
  tripId: number,
  page = 0,
  size = DEFAULT_PAGE_SIZE,
): Promise<Page<ActivityLogEntry>> {
  return request<Page<ActivityLogEntry>>(
    `/api/v1/trips/${tripId}/activity-log?page=${page}&size=${size}`,
  );
}
