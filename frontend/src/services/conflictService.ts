import type { TripConflicts } from '../types';
import { request } from './apiError';

/**
 * The warnings about the trip's itinerary: overlapping activities, activities outside the trip's
 * dates and a cost above the budget. Answers 403 for another user's trip and 404 when it does not exist.
 */
export function getConflicts(tripId: number): Promise<TripConflicts> {
  return request<TripConflicts>(`/api/v1/trips/${tripId}/conflicts`);
}
