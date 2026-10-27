import { describe, expect, test } from 'vitest';
import { dayNumber, isDayInTrip, tripDays } from './tripDays';

describe('B5 tripDays', () => {
  test('lists every day of the trip, both ends included', () => {
    expect(tripDays('2027-05-01', '2027-05-05')).toEqual([
      '2027-05-01',
      '2027-05-02',
      '2027-05-03',
      '2027-05-04',
      '2027-05-05',
    ]);
  });

  test('a one-day trip has one day', () => {
    expect(tripDays('2027-05-01', '2027-05-01')).toEqual(['2027-05-01']);
  });

  test('crosses months, years and a leap day', () => {
    expect(tripDays('2027-12-30', '2028-01-02')).toEqual([
      '2027-12-30',
      '2027-12-31',
      '2028-01-01',
      '2028-01-02',
    ]);
    expect(tripDays('2028-02-28', '2028-03-01')).toEqual([
      '2028-02-28',
      '2028-02-29',
      '2028-03-01',
    ]);
  });

  test('does not skip or repeat a day when the clocks change', () => {
    expect(tripDays('2027-03-27', '2027-03-29')).toEqual([
      '2027-03-27',
      '2027-03-28',
      '2027-03-29',
    ]);
    expect(tripDays('2027-10-30', '2027-11-01')).toEqual([
      '2027-10-30',
      '2027-10-31',
      '2027-11-01',
    ]);
  });

  test('a start after the end has no days', () => {
    expect(tripDays('2027-05-05', '2027-05-01')).toEqual([]);
  });

  test('knows whether a day belongs to the trip', () => {
    expect(isDayInTrip('2027-05-01', '2027-05-01', '2027-05-05')).toBe(true);
    expect(isDayInTrip('2027-05-05', '2027-05-01', '2027-05-05')).toBe(true);
    expect(isDayInTrip('2027-04-30', '2027-05-01', '2027-05-05')).toBe(false);
    expect(isDayInTrip('2027-05-06', '2027-05-01', '2027-05-05')).toBe(false);
  });

  test('numbers the days from 1', () => {
    expect(dayNumber('2027-05-01', '2027-05-01')).toBe(1);
    expect(dayNumber('2027-05-04', '2027-05-01')).toBe(4);
    expect(dayNumber('2027-11-01', '2027-10-30')).toBe(3);
  });
});
