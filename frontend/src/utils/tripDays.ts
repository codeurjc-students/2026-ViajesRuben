const DAY_MS = 24 * 60 * 60 * 1000;

function toUtc(isoDate: string): number {
  return Date.parse(`${isoDate}T00:00:00Z`);
}

/** Every day from `startDate` to `endDate`, both included, as ISO strings (yyyy-MM-dd). */
export function tripDays(startDate: string, endDate: string): string[] {
  const days: string[] = [];
  for (let time = toUtc(startDate); time <= toUtc(endDate); time += DAY_MS) {
    days.push(new Date(time).toISOString().slice(0, 10));
  }
  return days;
}

/** True when `day` is one of the days of the trip (ISO dates compare as text). */
export function isDayInTrip(day: string, startDate: string, endDate: string): boolean {
  return day >= startDate && day <= endDate;
}

/** "Day 1" is the first day of the trip. */
export function dayNumber(day: string, startDate: string): number {
  return Math.round((toUtc(day) - toUtc(startDate)) / DAY_MS) + 1;
}
