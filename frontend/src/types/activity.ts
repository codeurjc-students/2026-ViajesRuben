/** Mirrors the backend ActivityCategory enum. */
export type ActivityCategory =
  'SIGHTSEEING' | 'FOOD' | 'TRANSPORT' | 'ACCOMMODATION' | 'LEISURE' | 'OTHER';

/** Mirrors the backend ActivityDTO. The date is an ISO string (yyyy-MM-dd), the times "HH:mm:ss". */
export interface Activity {
  id: number;
  tripId: number;
  authorId: number;
  /** Who added it; null in a public itinerary, which never names the trip's members. */
  authorName: string | null;
  name: string;
  description: string | null;
  location: string | null;
  date: string;
  /** Null for an activity without schedule: it lasts the whole day. */
  startTime: string | null;
  endTime: string | null;
  cost: number | null;
  category: ActivityCategory | null;
  /** Null without picture; the picture is at `/api/v1/images/{imageId}`. */
  imageId: string | null;
}

/** Mirrors the backend ActivityRequest: what the form sends to create or replace an activity. Times are "HH:mm". */
export interface ActivityData {
  name: string;
  date: string;
  startTime: string | null;
  endTime: string | null;
  location: string | null;
  description: string | null;
  cost: number | null;
  category: ActivityCategory | null;
}

/** Mirrors the backend CategorySpendingDTO (I5). A null category is the activities without one. */
export interface CategorySpending {
  category: ActivityCategory | null;
  total: number;
}

/** Mirrors the backend DaySpendingDTO (I5): what was spent that day and the sum up to it. */
export interface DaySpending {
  /** ISO date (yyyy-MM-dd). */
  date: string;
  total: number;
  cumulative: number;
}

/**
 * Mirrors the backend TripSpendingDTO (I5): the data of the spending charts. `byCategory` has only
 * the categories with some cost, the biggest first; `byDay` has every day of the trip, 0 included.
 */
export interface TripSpending {
  /** Null when the trip has no budget. */
  budget: number | null;
  totalCost: number;
  byCategory: CategorySpending[];
  byDay: DaySpending[];
}

/** Mirrors the backend TripCostDTO: what the activities cost and what is left of the budget. */
export interface TripCost {
  totalCost: number;
  /** The budget minus the total cost, negative when over; null when the trip has no budget. */
  remainingBudget: number | null;
}
