import { useEffect, useState } from 'react';
import { ApiError } from '../services/apiError';
import { getActivity } from '../services/activityService';
import type { Activity } from '../types';

interface Loaded {
  key: string;
  activity?: Activity;
  error?: unknown;
}

export interface ActivityLoad {
  /** The activity, once it has arrived. */
  activity?: Activity;
  /** Why it could not be loaded (403, 404, no connection...); the caller rethrows it to the route's error page. */
  error?: unknown;
}

const isId = (raw: string | undefined): raw is string => raw !== undefined && /^\d+$/.test(raw);

/**
 * Loads one activity by the ids of the URL; a malformed id is an activity that does not exist. Pass
 * no activity id (a form that creates) to load nothing.
 */
export function useActivity(
  rawTripId: string | undefined,
  rawActivityId: string | undefined,
): ActivityLoad {
  const wanted = rawActivityId !== undefined;
  const valid = isId(rawTripId) && isId(rawActivityId);
  const key = `${rawTripId}/${rawActivityId}`;
  // The answer is kept with its ids, so a previous activity is never shown for another URL.
  const [loaded, setLoaded] = useState<Loaded>();

  useEffect(() => {
    if (!valid) return;
    let cancelled = false;
    getActivity(Number(rawTripId), Number(rawActivityId))
      .then((activity) => {
        if (!cancelled) setLoaded({ key, activity });
      })
      .catch((error) => {
        if (!cancelled) setLoaded({ key, error });
      });
    return () => {
      cancelled = true;
    };
  }, [key, valid, rawTripId, rawActivityId]);

  if (!wanted) return {};
  if (!valid) return { error: new ApiError(404) };
  return loaded?.key === key ? { activity: loaded.activity, error: loaded.error } : {};
}
