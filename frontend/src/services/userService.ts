import type { ImageInfo, PasswordChange, ProfileData, User } from '../types';
import { request, sendJson } from './apiError';

const usersUrl = (id: number) => `/api/v1/users/${id}`;

/** Answers 403 for another user's profile and 404 when the user does not exist. */
export function getProfile(id: number): Promise<User> {
  return request<User>(usersUrl(id));
}

/** Answers 409 when the email belongs to another account. */
export function updateProfile(id: number, data: ProfileData): Promise<User> {
  return sendJson<User>('PUT', usersUrl(id), data);
}

/** Answers 400 when the current password is wrong; the session stays open. */
export function changePassword(id: number, data: PasswordChange): Promise<void> {
  return sendJson<void>('PUT', `${usersUrl(id)}/password`, data);
}

/** Stores the picture as the avatar (creating or replacing it); 400 for anything but JPEG, PNG or WebP. */
export function uploadAvatar(id: number, file: File): Promise<ImageInfo> {
  const body = new FormData();
  body.append('file', file);
  return request<ImageInfo>(`${usersUrl(id)}/avatar`, { method: 'PUT', body });
}

export function deleteAvatar(id: number): Promise<void> {
  return request<void>(`${usersUrl(id)}/avatar`, { method: 'DELETE' });
}
