/** Mirrors the backend TripDTO. Dates are ISO strings (yyyy-MM-dd). */
export interface Trip {
  id: number;
  name: string;
  destination: string;
  startDate: string;
  endDate: string;
}
