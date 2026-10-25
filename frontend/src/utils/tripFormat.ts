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
