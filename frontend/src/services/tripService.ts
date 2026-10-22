import type { Trip } from '../types';
import { request } from './apiError';

export function getTrips(): Promise<Trip[]> {
  return request<Trip[]>('/api/v1/trips');
}
