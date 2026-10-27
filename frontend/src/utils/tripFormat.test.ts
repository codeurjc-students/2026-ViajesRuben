import { describe, expect, test } from 'vitest';
import {
  formatBudget,
  formatDateRange,
  formatDayLabel,
  formatDuration,
  formatTime,
  formatTripDate,
} from './tripFormat';

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

describe('B5 itinerary formats', () => {
  test('labels a day with its weekday, without shifting it', () => {
    expect(formatDayLabel('2027-07-10')).toMatch(/^sáb\.?,? 10 jul\.?$/);
  });

  test('shows the time without seconds', () => {
    expect(formatTime('09:30:00')).toBe('09:30');
    expect(formatTime('21:05')).toBe('21:05');
  });

  test('says how long an activity lasts', () => {
    expect(formatDuration('10:00:00', '12:30:00')).toBe('2 h 30 min');
    expect(formatDuration('10:00', '11:00')).toBe('1 h');
    expect(formatDuration('10:15', '11:00')).toBe('45 min');
  });
});
