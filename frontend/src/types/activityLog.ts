/** Mirrors the backend ActivityLogAction enum: what a member did to the itinerary (I4). */
export type ActivityLogAction = 'CREATED' | 'UPDATED' | 'DELETED';

/**
 * Mirrors the backend ActivityLogEntryDTO: one line of the trip's change log. The two names are
 * those at the time of the change, so the line still reads the same after the activity is deleted.
 */
export interface ActivityLogEntry {
  id: number;
  /** Null once the activity was deleted. */
  activityId: number | null;
  activityName: string;
  userId: number;
  userName: string;
  action: ActivityLogAction;
  /**
   * For an edit, the names of the fields that changed (`name`, `description`, `location`, `date`,
   * `startTime`, `endTime`, `cost`, `category`, `image`), never their values; empty otherwise.
   */
  changedFields: string[];
  /** ISO instant (`2026-11-18T10:15:30Z`). */
  createdAt: string;
}
