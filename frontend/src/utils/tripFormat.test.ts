import { expect, test } from 'vitest';
import { formatBudget, formatDateRange, formatTripDate } from './tripFormat';

test('B4 formats a date in Spanish without shifting the day', () => {
  expect(formatTripDate('2027-07-10')).toBe('10 jul 2027');
  expect(formatTripDate('2027-01-01')).toBe('1 ene 2027');
});

test('B4 formats a range of dates', () => {
  expect(formatDateRange('2027-07-10', '2027-07-17')).toBe('10 jul 2027 – 17 jul 2027');
});

test('B4 formats the budget in euros with the Spanish decimal comma', () => {
  expect(formatBudget(1200.5)).toMatch(/^1200,50\s€$|^1\.200,50\s€$/);
  expect(formatBudget(0)).toMatch(/^0,00\s€$/);
});
