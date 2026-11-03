import { test, expect, describe } from 'vitest';
import { ApiError, NetworkError } from '../services/apiError';
import { destinationAfterLogin, emailError, nameError, submitErrorCode } from './authForm';

describe('B2 authForm', () => {
  test('emailError accepts a normal address and returns a code otherwise', () => {
    expect(emailError('ana@example.com')).toBeUndefined();
    expect(emailError('   ')).toBe('required');
    expect(emailError('ana@')).toBe('invalid');
  });

  test('submitErrorCode maps each failure to a code', () => {
    expect(submitErrorCode(new ApiError(401))).toBe('badCredentials');
    expect(submitErrorCode(new ApiError(403))).toBe('blocked');
    expect(submitErrorCode(new ApiError(400))).toBe('invalidData');
    expect(submitErrorCode(new NetworkError())).toBe('network');
    expect(submitErrorCode(new ApiError(500))).toBe('server');
    expect(submitErrorCode(new Error('x'))).toBe('server');
  });

  test('destinationAfterLogin returns the page the visitor came from or the trip list', () => {
    expect(destinationAfterLogin(null)).toBe('/trips');
    expect(destinationAfterLogin({})).toBe('/trips');
    expect(
      destinationAfterLogin({ from: { pathname: '/trips/7', search: '?a=1', hash: '#x' } }),
    ).toBe('/trips/7?a=1#x');
  });

  test('B3 nameError asks for a name of at most 100 characters', () => {
    expect(nameError('Ana')).toBeUndefined();
    expect(nameError('x'.repeat(100))).toBeUndefined();
    expect(nameError('   ')).toBe('required');
    expect(nameError('x'.repeat(101))).toBe('tooLong');
  });
});
