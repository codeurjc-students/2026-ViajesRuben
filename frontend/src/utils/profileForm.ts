import { ApiError } from '../services/apiError';
import { MAX_PASSWORD_LENGTH, MIN_PASSWORD_LENGTH, submitErrorCode } from './authForm';

/** Why a profile request failed; the components turn each code into the Spanish message. */
export type ProfileError = 'sessionExpired' | 'forbidden' | 'network' | 'invalidData' | 'server';

/** The code of each wrong password field; only the fields that are wrong appear. */
export interface PasswordFieldErrors {
  current?: 'required';
  next?: 'required' | 'tooShort' | 'tooLong';
  confirmation?: 'required' | 'mismatch';
}

/** Code for a failed profile request; 401 and 403 mean something else here than at login. */
export function profileErrorCode(error: unknown): ProfileError {
  if (error instanceof ApiError) {
    if (error.status === 401) return 'sessionExpired';
    if (error.status === 403) return 'forbidden';
  }
  // Here 401 and 403 are already handled, so the login codes (bad credentials, blocked) cannot happen.
  const code = submitErrorCode(error);
  return code === 'network' || code === 'invalidData' ? code : 'server';
}

export function validatePasswordChange(
  current: string,
  next: string,
  confirmation: string,
): PasswordFieldErrors {
  const errors: PasswordFieldErrors = {};
  if (!current) errors.current = 'required';
  if (!next) errors.next = 'required';
  else if (next.length < MIN_PASSWORD_LENGTH) errors.next = 'tooShort';
  else if (next.length > MAX_PASSWORD_LENGTH) errors.next = 'tooLong';
  if (!confirmation) errors.confirmation = 'required';
  else if (confirmation !== next) errors.confirmation = 'mismatch';
  return errors;
}

/** "2026-10-01" as "1 de octubre de 2026"; built from the parts so the time zone cannot shift the day. */
export function formatMemberSince(isoDate: string): string {
  const [year, month, day] = isoDate.split('-').map(Number);
  return new Date(year, month - 1, day).toLocaleDateString('es-ES', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });
}
