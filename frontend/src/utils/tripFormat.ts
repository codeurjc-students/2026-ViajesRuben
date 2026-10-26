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
