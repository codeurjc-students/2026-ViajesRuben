import { describe, expect, test } from 'vitest';
import { activityOf } from '../testHelpers';
import {
  ACTIVITY_CATEGORIES,
  MAX_ACTIVITY_DESCRIPTION_LENGTH,
  MAX_ACTIVITY_NAME_LENGTH,
  MAX_LOCATION_LENGTH,
  emptyActivityValues,
  toActivityData,
  validateActivity,
  valuesFromActivity,
  type ActivityFormValues,
} from './activityForm';

const trip = { startDate: '2027-05-01', endDate: '2027-05-05' };
const valid: ActivityFormValues = { ...emptyActivityValues('2027-05-02'), name: 'Museo' };
const long = (length: number) => 'a'.repeat(length);

describe('B5 validateActivity', () => {
  test('accepts an activity with only a name and a day of the trip', () => {
    expect(validateActivity(valid, trip)).toEqual({});
  });

  test('requires the name and the date', () => {
    expect(validateActivity(emptyActivityValues(), trip)).toEqual({
      name: 'required',
      date: 'required',
    });
    expect(validateActivity({ ...valid, name: '   ' }, trip)).toEqual({ name: 'required' });
  });

  test('limits the length of the texts', () => {
    expect(validateActivity({ ...valid, name: long(MAX_ACTIVITY_NAME_LENGTH) }, trip)).toEqual({});
    expect(validateActivity({ ...valid, name: long(MAX_ACTIVITY_NAME_LENGTH + 1) }, trip)).toEqual({
      name: 'tooLong',
    });
    expect(validateActivity({ ...valid, location: long(MAX_LOCATION_LENGTH + 1) }, trip)).toEqual({
      location: 'tooLong',
    });
    expect(
      validateActivity({ ...valid, description: long(MAX_ACTIVITY_DESCRIPTION_LENGTH + 1) }, trip),
    ).toEqual({ description: 'tooLong' });
  });

  test('the date must be inside the trip, first and last day included', () => {
    expect(validateActivity({ ...valid, date: '2027-05-01' }, trip)).toEqual({});
    expect(validateActivity({ ...valid, date: '2027-05-05' }, trip)).toEqual({});
    expect(validateActivity({ ...valid, date: '2027-04-30' }, trip)).toEqual({
      date: 'outsideTrip',
    });
    expect(validateActivity({ ...valid, date: '2027-05-06' }, trip)).toEqual({
      date: 'outsideTrip',
    });
  });

  test('the end must be after the start', () => {
    const times = (startTime: string, endTime: string) => ({ ...valid, startTime, endTime });

    expect(validateActivity(times('10:00', '12:30'), trip)).toEqual({});
    expect(validateActivity(times('10:00', ''), trip)).toEqual({});
    expect(validateActivity(times('12:00', '10:00'), trip)).toEqual({
      endTime: 'endBeforeStart',
    });
    expect(validateActivity(times('10:00', '10:00'), trip)).toEqual({
      endTime: 'endBeforeStart',
    });
  });

  test('an end without a start is wrong', () => {
    expect(validateActivity({ ...valid, endTime: '10:00' }, trip)).toEqual({
      endTime: 'endWithoutStart',
    });
  });

  test('checks the cost like an amount of money', () => {
    expect(validateActivity({ ...valid, cost: '16,50' }, trip)).toEqual({});
    expect(validateActivity({ ...valid, cost: '0' }, trip)).toEqual({});
    expect(validateActivity({ ...valid, cost: 'mucho' }, trip)).toEqual({ cost: 'invalidAmount' });
    expect(validateActivity({ ...valid, cost: '-1' }, trip)).toEqual({ cost: 'negative' });
    expect(validateActivity({ ...valid, cost: '1,234' }, trip)).toEqual({ cost: 'invalidAmount' });
    expect(validateActivity({ ...valid, cost: '100000000' }, trip)).toEqual({ cost: 'tooBig' });
  });
});

describe('B5 activity form values', () => {
  test('an empty form starts on the given day', () => {
    expect(emptyActivityValues('2027-05-03')).toMatchObject({ date: '2027-05-03', name: '' });
    expect(emptyActivityValues().date).toBe('');
  });

  test('the form of an activity shows the times without seconds and empty texts for nulls', () => {
    const values = valuesFromActivity(
      activityOf({
        name: 'Museo',
        startTime: '09:30:00',
        endTime: '11:00:00',
        cost: 16.5,
        category: 'SIGHTSEEING',
        location: 'Alfama',
        description: 'Visita',
      }),
    );

    expect(values).toEqual({
      name: 'Museo',
      category: 'SIGHTSEEING',
      date: '2027-05-02',
      startTime: '09:30',
      endTime: '11:00',
      cost: '16.5',
      location: 'Alfama',
      description: 'Visita',
    });
    expect(valuesFromActivity(activityOf())).toMatchObject({
      category: '',
      startTime: '',
      endTime: '',
      cost: '',
      location: '',
      description: '',
    });
  });

  test('the request body trims the text and turns what is empty into null', () => {
    expect(
      toActivityData({
        ...valid,
        name: '  Museo ',
        cost: '16,5',
        category: 'FOOD',
        startTime: '09:30',
        endTime: '11:00',
        location: ' Alfama ',
        description: '  Visita ',
      }),
    ).toEqual({
      name: 'Museo',
      date: '2027-05-02',
      startTime: '09:30',
      endTime: '11:00',
      location: 'Alfama',
      description: 'Visita',
      cost: 16.5,
      category: 'FOOD',
    });
    expect(toActivityData(valid)).toEqual({
      name: 'Museo',
      date: '2027-05-02',
      startTime: null,
      endTime: null,
      location: null,
      description: null,
      cost: null,
      category: null,
    });
  });

  test('offers the same categories as the backend', () => {
    expect(ACTIVITY_CATEGORIES).toEqual([
      'SIGHTSEEING',
      'FOOD',
      'TRANSPORT',
      'ACCOMMODATION',
      'LEISURE',
      'OTHER',
    ]);
  });
});
