import { describe, expect, test } from 'vitest';
import { ApiError, NetworkError } from '../services/apiError';
import { formatMemberSince, profileErrorCode, validatePasswordChange } from './profileForm';

describe('B3 profileForm', () => {
  test('profileErrorCode tells 401 and 403 apart for a profile, not for a login', () => {
    expect(profileErrorCode(new ApiError(401))).toBe('sessionExpired');
    expect(profileErrorCode(new ApiError(403))).toBe('forbidden');
  });

  test('profileErrorCode keeps the shared codes for the other failures', () => {
    expect(profileErrorCode(new ApiError(400))).toBe('invalidData');
    expect(profileErrorCode(new NetworkError())).toBe('network');
    expect(profileErrorCode(new ApiError(500))).toBe('server');
    expect(profileErrorCode(new Error('x'))).toBe('server');
  });

  test('validatePasswordChange accepts a complete, matching form', () => {
    expect(validatePasswordChange('old-secret', 'new-secret', 'new-secret')).toEqual({});
  });

  test('validatePasswordChange asks for every empty field', () => {
    expect(validatePasswordChange('', '', '')).toEqual({
      current: 'required',
      next: 'required',
      confirmation: 'required',
    });
  });

  test('validatePasswordChange keeps the new password between 8 and 72 characters', () => {
    expect(validatePasswordChange('old', 'short', 'short').next).toBe('tooShort');
    expect(validatePasswordChange('old', 'x'.repeat(8), 'x'.repeat(8)).next).toBeUndefined();
    expect(validatePasswordChange('old', 'x'.repeat(72), 'x'.repeat(72)).next).toBeUndefined();
    expect(validatePasswordChange('old', 'x'.repeat(73), 'x'.repeat(73)).next).toBe('tooLong');
  });

  test('validatePasswordChange detects a confirmation that does not match', () => {
    expect(validatePasswordChange('old', 'new-secret', 'other-secret').confirmation).toBe(
      'mismatch',
    );
  });

  test('formatMemberSince writes the date in Spanish without shifting the day', () => {
    expect(formatMemberSince('2026-10-01')).toBe('1 de octubre de 2026');
    expect(formatMemberSince('2027-01-31')).toBe('31 de enero de 2027');
  });
});
