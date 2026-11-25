import { useEffect, useState } from 'react';
import { getMetrics } from '../services/adminMetricsService';
import type { PlatformMetrics } from '../types';

export interface PlatformMetricsLoad {
  /** The totals, once they have arrived. */
  metrics?: PlatformMetrics;
  /** Why they could not be loaded, so the page can hand it to the error page. */
  error?: unknown;
}

/** Loads the platform totals of the administrator's dashboard (I6) once. */
export function usePlatformMetrics(): PlatformMetricsLoad {
  const [load, setLoad] = useState<PlatformMetricsLoad>({});

  useEffect(() => {
    let cancelled = false;
    getMetrics()
      .then((metrics) => {
        if (!cancelled) setLoad({ metrics });
      })
      .catch((error: unknown) => {
        if (!cancelled) setLoad({ error });
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return load;
}
