import type { Page, Trip } from '../types';
import { request } from './apiError';

export const DEFAULT_PAGE_SIZE = 10;

export function getTrips(page = 0, size = DEFAULT_PAGE_SIZE): Promise<Page<Trip>> {
  return request<Page<Trip>>(`/api/v1/trips?page=${page}&size=${size}`);
}
