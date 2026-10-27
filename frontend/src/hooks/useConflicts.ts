import { useCallback, useEffect, useState } from 'react';
import { getConflicts } from '../services/conflictService';
import type { TripConflicts } from '../types';

interface Loaded {
  id: number;
  conflicts: TripConflicts;
}

export interface ConflictsLoad {
  /** The trip's conflicts, once they have arrived. */
  conflicts?: TripConflicts;
  /** Asks the server again, e.g. after an activity was deleted; the old warning stays until the answer. */
  reload: () => void;
}

/**
 * Loads the conflicts of a trip. A failure shows nothing: the warning is a help, and the page
 * already reports a trip that cannot be read.
 */
export function useConflicts(tripId: number | undefined): ConflictsLoad {
  // The answer is kept with its id, so another trip's warning is never shown.
  const [loaded, setLoaded] = useState<Loaded>();
  const [version, setVersion] = useState(0);

  useEffect(() => {
    if (tripId === undefined) return;
    let cancelled = false;
    getConflicts(tripId)
      .then((conflicts) => {
        if (!cancelled) setLoaded({ id: tripId, conflicts });
      })
      .catch(() => {
        // Nothing to show.
      });
    return () => {
      cancelled = true;
    };
  }, [tripId, version]);

  const reload = useCallback(() => setVersion((current) => current + 1), []);
  return { conflicts: loaded && loaded.id === tripId ? loaded.conflicts : undefined, reload };
}
