import type { Invitation, InvitationAnswer, Page, ReceivedInvitation } from '../types';
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

const RECEIVED_URL = '/api/v1/invitations';

/**
 * One page of the pending invitations addressed to the logged-in user, newest first. Answered
 * invitations are not listed.
 */
export function getMyInvitations(
  page = 0,
  size = DEFAULT_PAGE_SIZE,
): Promise<Page<ReceivedInvitation>> {
  return request<Page<ReceivedInvitation>>(`${RECEIVED_URL}?page=${page}&size=${size}`);
}

/**
 * The invitation the token of an email link points at, in any status. Answers 403 when it was sent
 * to another account and 404 when there is none, or its trip was deleted.
 */
export function getInvitationByToken(token: string): Promise<ReceivedInvitation> {
  return request<ReceivedInvitation>(`${RECEIVED_URL}/token/${encodeURIComponent(token)}`);
}

/**
 * Accepts or rejects an invitation; accepting makes the user a participant of the trip. Answers
 * 403 for another account, 404 when it does not exist and 409 when it was already answered.
 */
export function respondToInvitation(
  id: number,
  status: InvitationAnswer,
): Promise<ReceivedInvitation> {
  return sendJson<ReceivedInvitation>('PUT', `${RECEIVED_URL}/${id}/status`, { status });
}
