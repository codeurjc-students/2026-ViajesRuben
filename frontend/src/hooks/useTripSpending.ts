import { useCallback, useEffect, useState } from 'react';
import { getTripSpending } from '../services/activityService';
import type { TripSpending } from '../types';

interface Loaded {
  id: number;
  spending: TripSpending;
}

export interface TripSpendingLoad {
  /** The data of the spending charts, once it has arrived. */
  spending?: TripSpending;
  /** True when the server could not give it, so the page can say so instead of waiting. */
  failed: boolean;
  /** Asks the server again, e.g. after an activity was deleted; the old data stays until the answer. */
  reload: () => void;
}

/**
 * Loads the data of the spending charts (I5). The answer is kept with its trip id, so another
 * trip's figures are never shown.
 */
export function useTripSpending(tripId: number | undefined): TripSpendingLoad {
  const [loaded, setLoaded] = useState<Loaded>();
  const [failedId, setFailedId] = useState<number>();
  const [version, setVersion] = useState(0);

  useEffect(() => {
    if (tripId === undefined) return;
    let cancelled = false;
    getTripSpending(tripId)
      .then((spending) => {
        if (cancelled) return;
        setLoaded({ id: tripId, spending });
        setFailedId(undefined);
      })
      .catch(() => {
        if (!cancelled) setFailedId(tripId);
      });
    return () => {
      cancelled = true;
    };
  }, [tripId, version]);

  const reload = useCallback(() => setVersion((current) => current + 1), []);
  return {
    spending: loaded && loaded.id === tripId ? loaded.spending : undefined,
    failed: failedId !== undefined && failedId === tripId,
    reload,
  };
}
