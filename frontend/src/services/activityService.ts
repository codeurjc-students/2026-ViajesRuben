import type { Activity, ActivityData, CoverChange, ImageInfo, Page, TripCost } from '../types';
import { request, sendJson } from './apiError';
import { DEFAULT_PAGE_SIZE } from './tripService';

const activitiesUrl = (tripId: number) => `/api/v1/trips/${tripId}/activities`;
const activityUrl = (tripId: number, id: number) => `${activitiesUrl(tripId)}/${id}`;

/**
 * One page of the trip's activities in time order, only those of `date` (yyyy-MM-dd) when given.
 * Answers 400 for a bad date, 403 for another user's trip and 404 when the trip does not exist.
 */
export function getActivities(
  tripId: number,
  date?: string,
  page = 0,
  size = DEFAULT_PAGE_SIZE,
): Promise<Page<Activity>> {
  const day = date ? `&date=${date}` : '';
  return request<Page<Activity>>(`${activitiesUrl(tripId)}?page=${page}&size=${size}${day}`);
}

/**
 * What the trip's activities cost in total and what is left of its budget (null without budget).
 * Answers 403 for another user's trip and 404 when it does not exist.
 */
export function getTripCost(tripId: number): Promise<TripCost> {
  return request<TripCost>(`${activitiesUrl(tripId)}/cost`);
}

/** Answers 403 for another user's trip and 404 when the trip or the activity of that trip does not exist. */
export function getActivity(tripId: number, id: number): Promise<Activity> {
  return request<Activity>(activityUrl(tripId, id));
}

/** Creates the activity; answers 400 for invalid data or a date outside the trip, 403 or 404. */
export function createActivity(tripId: number, data: ActivityData): Promise<Activity> {
  return sendJson<Activity>('POST', activitiesUrl(tripId), data);
}

/** Replaces every field of the activity, not its picture; answers 400, 403 or 404. */
export function updateActivity(tripId: number, id: number, data: ActivityData): Promise<Activity> {
  return sendJson<Activity>('PUT', activityUrl(tripId, id), data);
}

/** Deletes the activity with its picture; answers 403 or 404. */
export function deleteActivity(tripId: number, id: number): Promise<void> {
  return request<void>(activityUrl(tripId, id), { method: 'DELETE' });
}

/** Stores the picture (creating or replacing it); 400 for anything but JPEG, PNG or WebP, 413 if too large. */
export function uploadActivityImage(tripId: number, id: number, file: File): Promise<ImageInfo> {
  const body = new FormData();
  body.append('file', file);
  return request<ImageInfo>(`${activityUrl(tripId, id)}/image`, { method: 'PUT', body });
}

export function deleteActivityImage(tripId: number, id: number): Promise<void> {
  return request<void>(`${activityUrl(tripId, id)}/image`, { method: 'DELETE' });
}

/** Applies the picture chosen in the form once the activity exists. */
export async function applyActivityImage(
  tripId: number,
  id: number,
  change: CoverChange,
): Promise<void> {
  if (change.kind === 'replace') await uploadActivityImage(tripId, id, change.file);
  else if (change.kind === 'remove') await deleteActivityImage(tripId, id);
}
