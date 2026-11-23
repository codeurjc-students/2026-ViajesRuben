import type { Location } from 'react-router';
import { ApiError, NetworkError } from '../services/apiError';

export const MIN_PASSWORD_LENGTH = 8;
/** BCrypt only reads the first 72 bytes, so the backend refuses longer passwords. */
export const MAX_PASSWORD_LENGTH = 72;
export const MAX_NAME_LENGTH = 100;

/**
 * Whether the text looks like an address: no whitespace, one `@` with something before it, and a
 * dot in the domain with something before and after it. Checked with string methods because the
 * equivalent regular expression backtracks (super-linear time) on long input.
 */
function hasEmailShape(email: string): boolean {
  if (/\s/.test(email)) return false;
  const at = email.indexOf('@');
  if (at < 1 || email.includes('@', at + 1)) return false;
  const domain = email.slice(at + 1);
  const dot = domain.indexOf('.', 1);
  return dot !== -1 && dot < domain.length - 1;
}

/** Why a field or a request is wrong; the components turn each code into the Spanish message. */
export type EmailError = 'required' | 'invalid';
export type NameError = 'required' | 'tooLong';
export type SubmitError = 'network' | 'invalidData' | 'badCredentials' | 'blocked' | 'server';

export function emailError(email: string): EmailError | undefined {
  if (!email.trim()) return 'required';
  if (!hasEmailShape(email.trim())) return 'invalid';
  return undefined;
}

export function nameError(name: string): NameError | undefined {
  if (!name.trim()) return 'required';
  if (name.trim().length > MAX_NAME_LENGTH) return 'tooLong';
  return undefined;
}

/** Code for a failure that does not belong to a single field. */
export function submitErrorCode(error: unknown): SubmitError {
  if (error instanceof NetworkError) return 'network';
  if (error instanceof ApiError) {
    if (error.status === 400) return 'invalidData';
    if (error.status === 401) return 'badCredentials';
    if (error.status === 403) return 'blocked';
  }
  return 'server';
}

/** The page a visitor was sent away from by RequireAuth, or the trip list. */
export function destinationAfterLogin(state: unknown): string {
  const from = (state as { from?: Location } | null)?.from;
  return from ? `${from.pathname}${from.search}${from.hash}` : '/trips';
}
