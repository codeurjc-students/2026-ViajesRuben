import type { Trip } from '../types';

const API_URL = import.meta.env.VITE_API_URL ?? '';

export async function getTrips(): Promise<Trip[]> {
  const response = await fetch(`${API_URL}/api/v1/trips`);
  return response.json();
}
