/** Mirrors the backend OverlapDTO: two activities of the same day whose times intersect. Times are "HH:mm:ss". */
export interface Overlap {
  date: string;
  firstId: number;
  firstName: string;
  firstStart: string;
  firstEnd: string;
  secondId: number;
  secondName: string;
  secondStart: string;
  secondEnd: string;
}

/** Mirrors the backend OutsideDatesDTO: an activity on a day before the trip starts or after it ends. */
export interface OutsideDates {
  activityId: number;
  name: string;
  date: string;
}

/** Mirrors the backend BudgetConflictDTO: the activities cost more than the budget. */
export interface BudgetConflict {
  totalCost: number;
  budget: number;
}

/** Mirrors the backend TripConflictsDTO; `budget` is null when the total is within the budget. */
export interface TripConflicts {
  overlaps: Overlap[];
  outsideDates: OutsideDates[];
  budget: BudgetConflict | null;
}
