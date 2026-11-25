import type { PlatformMetrics } from '../types';
import { request } from './apiError';

const METRICS_URL = '/api/v1/admin/metrics';

/**
 * The platform totals (users, trips and activities) for the administrator's dashboard; answers 401
 * without a session and 403 for anyone but the administrator.
 */
export function getMetrics(): Promise<PlatformMetrics> {
  return request<PlatformMetrics>(METRICS_URL);
}
