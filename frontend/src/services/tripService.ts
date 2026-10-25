import type { CoverChange, ImageInfo, Page, Trip, TripData } from '../types';
import { request, sendJson } from './apiError';

export const DEFAULT_PAGE_SIZE = 10;

const TRIPS_URL = '/api/v1/trips';
const tripUrl = (id: number) => `${TRIPS_URL}/${id}`;

/** The logged-in user's own trips; answers 401 without a session. */
export function getTrips(page = 0, size = DEFAULT_PAGE_SIZE): Promise<Page<Trip>> {
  return request<Page<Trip>>(`${TRIPS_URL}?page=${page}&size=${size}`);
}

/** Answers 403 for another user's trip and 404 when it does not exist. */
export function getTrip(id: number): Promise<Trip> {
  return request<Trip>(tripUrl(id));
}

/** Creates the trip for the logged-in user; answers 400 for invalid data, e.g. the end before the start. */
export function createTrip(data: TripData): Promise<Trip> {
  return sendJson<Trip>('POST', TRIPS_URL, data);
}

/** Replaces every field of the trip, not its cover; answers 400, 403 or 404. */
export function updateTrip(id: number, data: TripData): Promise<Trip> {
  return sendJson<Trip>('PUT', tripUrl(id), data);
}

/** Deletes the trip with its cover; answers 403 or 404. */
export function deleteTrip(id: number): Promise<void> {
  return request<void>(tripUrl(id), { method: 'DELETE' });
}

/** Stores the picture as the cover (creating or replacing it); 400 for anything but JPEG, PNG or WebP, 413 if too large. */
export function uploadCover(id: number, file: File): Promise<ImageInfo> {
  const body = new FormData();
  body.append('file', file);
  return request<ImageInfo>(`${tripUrl(id)}/cover`, { method: 'PUT', body });
}

export function deleteCover(id: number): Promise<void> {
  return request<void>(`${tripUrl(id)}/cover`, { method: 'DELETE' });
}

/** Applies the cover chosen in the form once the trip exists. */
export async function applyCover(id: number, change: CoverChange): Promise<void> {
  if (change.kind === 'replace') await uploadCover(id, change.file);
  else if (change.kind === 'remove') await deleteCover(id);
}
