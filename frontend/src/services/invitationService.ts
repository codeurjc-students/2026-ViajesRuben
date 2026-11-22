import type { Invitation, Page } from '../types';
import { request, sendJson } from './apiError';
import { DEFAULT_PAGE_SIZE } from './tripService';

const invitationsUrl = (tripId: number) => `/api/v1/trips/${tripId}/invitations`;

/**
 * One page of the trip's invitations, newest first. Only the trip's creator: answers 403 for anyone
 * else, the administrator included, and 404 when the trip does not exist.
 */
export function getInvitations(
  tripId: number,
  page = 0,
  size = DEFAULT_PAGE_SIZE,
): Promise<Page<Invitation>> {
  return request<Page<Invitation>>(`${invitationsUrl(tripId)}?page=${page}&size=${size}`);
}

/**
 * Invites an email to the trip, which also sends it an email with a link. The email may have no
 * account yet. Answers 400 for an invalid or the creator's own email, 403, 404, and 409 when that
 * email already has a pending invitation to the trip.
 */
export function createInvitation(tripId: number, email: string): Promise<Invitation> {
  return sendJson<Invitation>('POST', invitationsUrl(tripId), { email });
}
