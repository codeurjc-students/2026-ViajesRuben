import { describe, expect, test } from 'vitest';
import { tripOf } from '../testHelpers';
import {
  EMPTY_TRIP_VALUES,
  MAX_BUDGET,
  MAX_DESCRIPTION_LENGTH,
  MAX_DESTINATION_LENGTH,
  MAX_TRIP_NAME_LENGTH,
  toTripData,
  validateTrip,
  valuesFromTrip,
  type TripFormValues,
} from './tripForm';

const valid: TripFormValues = {
  name: 'Escapada',
  destination: 'Lisboa',
  startDate: '2027-05-01',
  endDate: '2027-05-05',
  budget: '',
  description: '',
};

describe('B4 validateTrip', () => {
  test('accepts a trip with only the required fields and a one-day trip', () => {
    expect(validateTrip(valid)).toEqual({});
    expect(validateTrip({ ...valid, endDate: valid.startDate })).toEqual({});
  });

  test('requires every mandatory field', () => {
    expect(validateTrip(EMPTY_TRIP_VALUES)).toEqual({
      name: 'required',
      destination: 'required',
      startDate: 'required',
      endDate: 'required',
    });
    expect(validateTrip({ ...valid, name: '   ' })).toEqual({ name: 'required' });
  });

  test('limits the length of the texts', () => {
    expect(validateTrip({ ...valid, name: 'n'.repeat(MAX_TRIP_NAME_LENGTH + 1) })).toEqual({
      name: 'tooLong',
    });
    expect(validateTrip({ ...valid, destination: 'd'.repeat(MAX_DESTINATION_LENGTH + 1) })).toEqual(
      { destination: 'tooLong' },
    );
    expect(validateTrip({ ...valid, description: 'x'.repeat(MAX_DESCRIPTION_LENGTH + 1) })).toEqual(
      { description: 'tooLong' },
    );
    expect(
      validateTrip({
        ...valid,
        name: 'n'.repeat(MAX_TRIP_NAME_LENGTH),
        description: 'x'.repeat(MAX_DESCRIPTION_LENGTH),
      }),
    ).toEqual({});
  });

  test('the end date cannot be before the start date, and only when both exist', () => {
    expect(validateTrip({ ...valid, endDate: '2027-04-30' })).toEqual({ endDate: 'beforeStart' });
    expect(validateTrip({ ...valid, startDate: '' })).toEqual({ startDate: 'required' });
  });

  test.each([
    ['1200', undefined],
    ['1200.5', undefined],
    ['1200,50', undefined],
    ['0', undefined],
    [' 10 ', undefined],
    ['', undefined],
    ['-5', 'negative'],
    ['abc', 'invalidAmount'],
    ['1e3', 'invalidAmount'],
    ['10.123', 'invalidAmount'],
    ['1.', 'invalidAmount'],
    [String(MAX_BUDGET + 1), 'tooBig'],
    [String(MAX_BUDGET), undefined],
  ])('budget "%s" -> %s', (budget, expected) => {
    const errors = validateTrip({ ...valid, budget });

    expect(errors.budget).toBe(expected);
  });
});

describe('B4 toTripData and valuesFromTrip', () => {
  test('trims the texts and turns an empty budget and description into null', () => {
    expect(toTripData({ ...valid, name: ' Escapada ', destination: ' Lisboa ' })).toEqual({
      name: 'Escapada',
      destination: 'Lisboa',
      startDate: '2027-05-01',
      endDate: '2027-05-05',
      budget: null,
      description: null,
    });
  });

  test('turns a comma budget into a number and keeps the description', () => {
    expect(toTripData({ ...valid, budget: '1200,50', description: ' Sol ' })).toMatchObject({
      budget: 1200.5,
      description: 'Sol',
    });
  });

  test('a stored trip becomes the text of the form and back', () => {
    const trip = tripOf({ budget: 300.5, description: 'Ruinas' });

    const values = valuesFromTrip(trip);

    expect(values).toEqual({
      name: 'Escapada',
      destination: 'Lisboa',
      startDate: '2027-05-01',
      endDate: '2027-05-05',
      budget: '300.5',
      description: 'Ruinas',
    });
    expect(toTripData(values)).toMatchObject({ budget: 300.5, description: 'Ruinas' });
    expect(valuesFromTrip(tripOf())).toMatchObject({ budget: '', description: '' });
  });
});
