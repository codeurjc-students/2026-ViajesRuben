import { useCallback, useEffect, useState } from 'react';
import { getTripCost } from '../services/activityService';
import type { TripCost } from '../types';

interface Loaded {
  id: number;
  cost: TripCost;
}

export interface TripCostLoad {
  /** What the activities cost and what is left of the budget, once it has arrived. */
  cost?: TripCost;
  /** Asks the server again, e.g. after an activity was deleted; the old figure stays until the answer. */
  reload: () => void;
}

/**
 * Loads what the trip's activities cost. A failure shows nothing: the figure is a help, and the
 * page already reports a trip that cannot be read.
 */
export function useTripCost(tripId: number | undefined): TripCostLoad {
  // The answer is kept with its id, so another trip's figure is never shown.
  const [loaded, setLoaded] = useState<Loaded>();
  const [version, setVersion] = useState(0);

  useEffect(() => {
    if (tripId === undefined) return;
    let cancelled = false;
    getTripCost(tripId)
      .then((cost) => {
        if (!cancelled) setLoaded({ id: tripId, cost });
      })
      .catch(() => {
        // Nothing to show.
      });
    return () => {
      cancelled = true;
    };
  }, [tripId, version]);

  const reload = useCallback(() => setVersion((current) => current + 1), []);
  return { cost: loaded && loaded.id === tripId ? loaded.cost : undefined, reload };
}
