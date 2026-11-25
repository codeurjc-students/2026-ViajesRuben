/**
 * Mirrors the backend PlatformMetricsDTO: the totals on the administrator's dashboard. Users are the
 * regular accounts (blocked ones included, the administrator not); trips and activities are all.
 */
export interface PlatformMetrics {
  users: number;
  trips: number;
  activities: number;
}
