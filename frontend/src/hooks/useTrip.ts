import { useEffect, useState } from 'react';
import { ApiError } from '../services/apiError';
import { getTrip } from '../services/tripService';
import type { Trip } from '../types';

interface Loaded<T> {
  id: number;
  trip?: T;
  error?: unknown;
}

export interface TripLoad<T = Trip> {
  /** The trip, once it has arrived. */
  trip?: T;
  /** Why it could not be loaded (403, 404, no connection...); the caller rethrows it to the route's error page. */
  error?: unknown;
}

/**
 * Loads one trip by the id of the URL; an id that is not a whole number is a trip that does not
 * exist. `load` must keep the same identity between renders; it is `getTrip` unless the page reads
 * another source, such as the public trips.
 */
export function useTrip(rawId: string | undefined): TripLoad;
export function useTrip<T>(
  rawId: string | undefined,
  load: (id: number) => Promise<T>,
): TripLoad<T>;
export function useTrip<T>(
  rawId: string | undefined,
  load?: (id: number) => Promise<T>,
): TripLoad<T> {
  const fetchTrip = (load ?? getTrip) as (id: number) => Promise<T>;
  const id = Number(rawId);
  const valid = rawId !== undefined && /^\d+$/.test(rawId);
  // The answer is kept with its id, so a previous trip is never shown for another URL.
  const [loaded, setLoaded] = useState<Loaded<T>>();

  useEffect(() => {
    if (!valid) return;
    let cancelled = false;
    fetchTrip(id)
      .then((trip) => {
        if (!cancelled) setLoaded({ id, trip });
      })
      .catch((error) => {
        if (!cancelled) setLoaded({ id, error });
      });
    return () => {
      cancelled = true;
    };
  }, [id, valid, fetchTrip]);

  if (!valid) return { error: new ApiError(404) };
  return loaded?.id === id ? { trip: loaded.trip, error: loaded.error } : {};
}
