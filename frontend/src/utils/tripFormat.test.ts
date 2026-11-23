import { describe, expect, test } from 'vitest';
import {
  formatBudget,
  formatDateRange,
  formatDateTime,
  formatDayLabel,
  formatDuration,
  formatRelativeTime,
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

describe('I4 change log formats', () => {
  const now = new Date('2027-07-10T12:00:00Z');
  const ago = (seconds: number) => new Date(now.getTime() - seconds * 1000).toISOString();

  test('says how long ago something happened, in the unit that fits', () => {
    expect(formatRelativeTime(ago(0), now)).toBe('ahora');
    expect(formatRelativeTime(ago(30), now)).toBe('hace 30 s');
    expect(formatRelativeTime(ago(5 * 60), now)).toBe('hace 5 min');
    expect(formatRelativeTime(ago(2 * 3600 + 120), now)).toBe('hace 2 h');
    expect(formatRelativeTime(ago(24 * 3600), now)).toBe('ayer');
    expect(formatRelativeTime(ago(3 * 24 * 3600), now)).toBe('hace 3 d');
  });

  test('rounds down: 59 seconds is not yet a minute, 119 minutes is not yet two hours', () => {
    expect(formatRelativeTime(ago(59), now)).toBe('hace 59 s');
    expect(formatRelativeTime(ago(119 * 60), now)).toBe('hace 1 h');
  });

  test('a month or more ago it gives the date instead', () => {
    expect(formatRelativeTime('2027-05-01T10:00:00Z', now)).toBe('1 may 2027');
    expect(formatRelativeTime(ago(29 * 24 * 3600), now)).toBe('hace 29 d');
  });

  test('an instant a little in the future counts as now', () => {
    expect(formatRelativeTime(ago(-20), now)).toBe('ahora');
  });

  test('uses the current time when none is given', () => {
    expect(formatRelativeTime(new Date().toISOString())).toBe('ahora');
  });

  test('gives the date and time for the tooltip', () => {
    expect(formatDateTime('2027-07-10T12:30:00Z')).toMatch(/^10 jul 2027,? \d{1,2}:\d{2}$/);
  });
});
