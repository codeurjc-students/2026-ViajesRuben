const dateFormat = new Intl.DateTimeFormat('es-ES', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  timeZone: 'UTC',
});

const budgetFormat = new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' });

/** "2027-07-10" as "10 jul 2027". */
export function formatTripDate(isoDate: string): string {
  return dateFormat.format(new Date(isoDate));
}

/** "10 jul 2027 – 17 jul 2027". */
export function formatDateRange(startDate: string, endDate: string): string {
  return `${formatTripDate(startDate)} – ${formatTripDate(endDate)}`;
}

/** 1200.5 as "1200,50 €". */
export function formatBudget(amount: number): string {
  return budgetFormat.format(amount);
}

const dayLabelFormat = new Intl.DateTimeFormat('es-ES', {
  weekday: 'short',
  day: 'numeric',
  month: 'short',
  timeZone: 'UTC',
});

/** "2027-07-10" as "sáb, 10 jul". */
export function formatDayLabel(isoDate: string): string {
  return dayLabelFormat.format(new Date(isoDate));
}

const shortDayFormat = new Intl.DateTimeFormat('es-ES', {
  day: 'numeric',
  month: 'short',
  timeZone: 'UTC',
});

/** "2027-07-10" as "10 jul", short enough for the axis of a chart. */
export function formatShortDay(isoDate: string): string {
  return shortDayFormat.format(new Date(isoDate));
}

const relativeFormat = new Intl.RelativeTimeFormat('es-ES', { numeric: 'auto', style: 'short' });
const dateTimeFormat = new Intl.DateTimeFormat('es-ES', {
  dateStyle: 'medium',
  timeStyle: 'short',
});

const MINUTE = 60;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;
/** Beyond this the date says more than "hace 45 d". */
const RELATIVE_LIMIT = 30 * DAY;

/**
 * How long ago an instant was, in the words of the browser's Spanish formatter ("now", "5 min ago",
 * "2 h ago", "yesterday", "3 d ago", in Spanish). A month or more ago it is the date, formatted like
 * `formatTripDate`. An instant in the future (a clock a little ahead) counts as now.
 */
export function formatRelativeTime(isoInstant: string, now: Date = new Date()): string {
  const seconds = Math.max(0, Math.floor((now.getTime() - new Date(isoInstant).getTime()) / 1000));
  if (seconds < MINUTE) return relativeFormat.format(-seconds, 'second');
  if (seconds < HOUR) return relativeFormat.format(-Math.floor(seconds / MINUTE), 'minute');
  if (seconds < DAY) return relativeFormat.format(-Math.floor(seconds / HOUR), 'hour');
  if (seconds < RELATIVE_LIMIT) return relativeFormat.format(-Math.floor(seconds / DAY), 'day');
  return formatTripDate(isoInstant);
}

/** An instant as the date and time of the user's clock, in Spanish: day, short month, year, time. */
export function formatDateTime(isoInstant: string): string {
  return dateTimeFormat.format(new Date(isoInstant));
}

/** "09:30:00" or "09:30" as "09:30". */
export function formatTime(time: string): string {
  return time.slice(0, 5);
}

/** The time between two "HH:mm" times of the same day: "2 h 30 min", "1 h", "45 min". */
export function formatDuration(startTime: string, endTime: string): string {
  const minutes = (time: string) => Number(time.slice(0, 2)) * 60 + Number(time.slice(3, 5));
  const total = minutes(endTime) - minutes(startTime);
  const hours = Math.floor(total / 60);
  const rest = total % 60;
  return [hours ? `${hours} h` : '', rest ? `${rest} min` : ''].filter(Boolean).join(' ');
}
