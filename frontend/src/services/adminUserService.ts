import type { AdminUser, Page, ProfileData, UserStatus } from '../types';
import { request, sendJson } from './apiError';
import { DEFAULT_PAGE_SIZE } from './tripService';

const USERS_URL = '/api/v1/admin/users';

/**
 * One page of the regular users whose name or email contains `search` (all of them when it is
 * blank); answers 401 without a session and 403 for anyone but the administrator.
 */
export function getUsers(
  search: string,
  page = 0,
  size = DEFAULT_PAGE_SIZE,
): Promise<Page<AdminUser>> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (search.trim()) params.set('search', search.trim());
  return request<Page<AdminUser>>(`${USERS_URL}?${params}`);
}

/** Answers 409 when the email belongs to another account; 403 for the administrator's own account. */
export function updateUser(id: number, data: ProfileData): Promise<AdminUser> {
  return sendJson<AdminUser>('PUT', `${USERS_URL}/${id}`, data);
}

/** Blocks (`BLOCKED`) or unblocks (`ACTIVE`) the account; 403 for the administrator's own account. */
export function setUserStatus(id: number, status: UserStatus): Promise<AdminUser> {
  return sendJson<AdminUser>('PUT', `${USERS_URL}/${id}/status`, { status });
}
