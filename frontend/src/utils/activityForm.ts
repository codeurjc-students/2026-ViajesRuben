import type { Activity, ActivityCategory, ActivityData } from '../types';
import { amountError, normalizeAmount, type AmountError } from './tripForm';
import { isDayInTrip } from './tripDays';

export const MAX_ACTIVITY_NAME_LENGTH = 100;
export const MAX_LOCATION_LENGTH = 200;
export const MAX_ACTIVITY_DESCRIPTION_LENGTH = 2000;

/** The same order as the backend enum; the component translates each one. */
export const ACTIVITY_CATEGORIES: readonly ActivityCategory[] = [
  'SIGHTSEEING',
  'FOOD',
  'TRANSPORT',
  'ACCOMMODATION',
  'LEISURE',
  'OTHER',
];

/** The text of every field of the form, as typed. Times are "HH:mm"; no category is the empty text. */
export interface ActivityFormValues {
  name: string;
  category: ActivityCategory | '';
  date: string;
  startTime: string;
  endTime: string;
  cost: string;
  location: string;
  description: string;
}

/** Why a field is wrong; the components turn each code into the Spanish message. */
export type ActivityFieldError =
  'required' | 'tooLong' | 'outsideTrip' | 'endBeforeStart' | 'endWithoutStart' | AmountError;

export type ActivityFieldErrors = Partial<Record<keyof ActivityFormValues, ActivityFieldError>>;

/** The days of the trip that contain the activity. */
export interface TripRange {
  startDate: string;
  endDate: string;
}

/** An empty form that starts on `date`. */
export function emptyActivityValues(date = ''): ActivityFormValues {
  return {
    name: '',
    category: '',
    date,
    startTime: '',
    endTime: '',
    cost: '',
    location: '',
    description: '',
  };
}

/** The API sends times as "09:30:00"; the time input wants "09:30". */
function toInputTime(time: string | null): string {
  return time ? time.slice(0, 5) : '';
}

export function valuesFromActivity(activity: Activity): ActivityFormValues {
  return {
    name: activity.name,
    category: activity.category ?? '',
    date: activity.date,
    startTime: toInputTime(activity.startTime),
    endTime: toInputTime(activity.endTime),
    cost: activity.cost === null ? '' : String(activity.cost),
    location: activity.location ?? '',
    description: activity.description ?? '',
  };
}

function dateError(values: ActivityFormValues, trip: TripRange): ActivityFieldError | undefined {
  if (!values.date) return 'required';
  return isDayInTrip(values.date, trip.startDate, trip.endDate) ? undefined : 'outsideTrip';
}

function endTimeError(values: ActivityFormValues): ActivityFieldError | undefined {
  if (!values.endTime) return undefined;
  if (!values.startTime) return 'endWithoutStart';
  // "HH:mm" strings compare in clock order; an end equal to the start is not after it.
  return values.endTime <= values.startTime ? 'endBeforeStart' : undefined;
}

/** The same rules as the backend's ActivityRequest and the trip's dates; only wrong fields appear. */
export function validateActivity(values: ActivityFormValues, trip: TripRange): ActivityFieldErrors {
  const name = values.name.trim();
  const found: ActivityFieldErrors = {
    name: !name ? 'required' : name.length > MAX_ACTIVITY_NAME_LENGTH ? 'tooLong' : undefined,
    date: dateError(values, trip),
    endTime: endTimeError(values),
    cost: amountError(values.cost),
    location: values.location.trim().length > MAX_LOCATION_LENGTH ? 'tooLong' : undefined,
    description:
      values.description.length > MAX_ACTIVITY_DESCRIPTION_LENGTH ? 'tooLong' : undefined,
  };
  return Object.fromEntries(
    Object.entries(found).filter(([, error]) => error),
  ) as ActivityFieldErrors;
}

/** The request body for valid values: trimmed text, null for what is empty, a number for the cost. */
export function toActivityData(values: ActivityFormValues): ActivityData {
  const cost = normalizeAmount(values.cost);
  return {
    name: values.name.trim(),
    date: values.date,
    startTime: values.startTime || null,
    endTime: values.endTime || null,
    location: values.location.trim() || null,
    description: values.description.trim() || null,
    cost: cost ? Number(cost) : null,
    category: values.category || null,
  };
}
