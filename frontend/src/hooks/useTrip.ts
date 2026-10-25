import { useEffect, useState } from 'react';
import { ApiError } from '../services/apiError';
import { getTrip } from '../services/tripService';
import type { Trip } from '../types';

interface Loaded {
  id: number;
  trip?: Trip;
  error?: unknown;
}

export interface TripLoad {
  /** The trip, once it has arrived. */
  trip?: Trip;
  /** Why it could not be loaded (403, 404, no connection...); the caller rethrows it to the route's error page. */
  error?: unknown;
}

/** Loads one trip by the id of the URL; an id that is not a whole number is a trip that does not exist. */
export function useTrip(rawId: string | undefined): TripLoad {
  const id = Number(rawId);
  const valid = rawId !== undefined && /^\d+$/.test(rawId);
  // The answer is kept with its id, so a previous trip is never shown for another URL.
  const [loaded, setLoaded] = useState<Loaded>();

  useEffect(() => {
    if (!valid) return;
    let cancelled = false;
    getTrip(id)
      .then((trip) => {
        if (!cancelled) setLoaded({ id, trip });
      })
      .catch((error) => {
        if (!cancelled) setLoaded({ id, error });
      });
    return () => {
      cancelled = true;
    };
  }, [id, valid]);

  if (!valid) return { error: new ApiError(404) };
  return loaded?.id === id ? { trip: loaded.trip, error: loaded.error } : {};
}
