import type { Trip, TripData } from '../types';

export const MAX_TRIP_NAME_LENGTH = 100;
export const MAX_DESTINATION_LENGTH = 100;
export const MAX_DESCRIPTION_LENGTH = 2000;
/** The backend stores up to 8 integer digits and 2 decimals. */
export const MAX_BUDGET = 99_999_999.99;

/** The text of every field of the form, as typed. */
export interface TripFormValues {
  name: string;
  destination: string;
  startDate: string;
  endDate: string;
  budget: string;
  description: string;
}

/** Why a field is wrong; the components turn each code into the Spanish message. */
export type TripFieldError =
  'required' | 'tooLong' | 'beforeStart' | 'invalidAmount' | 'negative' | 'tooBig';

export type TripFieldErrors = Partial<Record<keyof TripFormValues, TripFieldError>>;

export const EMPTY_TRIP_VALUES: TripFormValues = {
  name: '',
  destination: '',
  startDate: '',
  endDate: '',
  budget: '',
  description: '',
};

export function valuesFromTrip(trip: Trip): TripFormValues {
  return {
    name: trip.name,
    destination: trip.destination,
    startDate: trip.startDate,
    endDate: trip.endDate,
    budget: trip.budget === null ? '' : String(trip.budget),
    description: trip.description ?? '',
  };
}

/** "1200,50" and "1200.50" are the same amount; empty is no budget. */
function normalizeBudget(text: string): string {
  return text.trim().replace(',', '.');
}

function budgetError(text: string): TripFieldError | undefined {
  const normalized = normalizeBudget(text);
  if (!normalized) return undefined;
  const amount = Number(normalized);
  if (Number.isNaN(amount)) return 'invalidAmount';
  if (amount < 0) return 'negative';
  if (!/^\d+(\.\d{1,2})?$/.test(normalized)) return 'invalidAmount';
  if (amount > MAX_BUDGET) return 'tooBig';
  return undefined;
}

function requiredText(text: string, max: number): TripFieldError | undefined {
  if (!text.trim()) return 'required';
  if (text.trim().length > max) return 'tooLong';
  return undefined;
}

/** The same rules as the backend's TripRequest; only the fields that are wrong appear in the result. */
export function validateTrip(values: TripFormValues): TripFieldErrors {
  const found: TripFieldErrors = {
    name: requiredText(values.name, MAX_TRIP_NAME_LENGTH),
    destination: requiredText(values.destination, MAX_DESTINATION_LENGTH),
    startDate: values.startDate ? undefined : 'required',
    endDate: values.endDate ? undefined : 'required',
    budget: budgetError(values.budget),
    description: values.description.length > MAX_DESCRIPTION_LENGTH ? 'tooLong' : undefined,
  };
  if (!found.endDate && !found.startDate && values.endDate < values.startDate) {
    found.endDate = 'beforeStart';
  }
  return Object.fromEntries(Object.entries(found).filter(([, error]) => error)) as TripFieldErrors;
}

/** The request body for valid values: trimmed text, a number or null for the budget, null for no description. */
export function toTripData(values: TripFormValues): TripData {
  const budget = normalizeBudget(values.budget);
  const description = values.description.trim();
  return {
    name: values.name.trim(),
    destination: values.destination.trim(),
    startDate: values.startDate,
    endDate: values.endDate,
    budget: budget ? Number(budget) : null,
    description: description || null,
  };
}
