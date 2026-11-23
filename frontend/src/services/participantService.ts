import type { Page, Participant } from '../types';
import { request } from './apiError';
import { DEFAULT_PAGE_SIZE } from './tripService';

const participantsUrl = (tripId: number) => `/api/v1/trips/${tripId}/participants`;

/**
 * One page of the people who joined the trip, oldest first. Anyone who can see the trip may ask:
 * answers 403 for anyone else and 404 when the trip does not exist. Only the creator receives the
 * emails.
 */
export function getParticipants(
  tripId: number,
  page = 0,
  size = DEFAULT_PAGE_SIZE,
): Promise<Page<Participant>> {
  return request<Page<Participant>>(`${participantsUrl(tripId)}?page=${page}&size=${size}`);
}

/**
 * Removes a participant from the trip: the creator removes anyone, a participant only themselves,
 * which is leaving. Answers 403 for anyone else (the administrator included), 404 when the trip or
 * the participant does not exist, and 409 when the user is the creator.
 */
export function removeParticipant(tripId: number, userId: number): Promise<void> {
  return request<void>(`${participantsUrl(tripId)}/${userId}`, { method: 'DELETE' });
}
