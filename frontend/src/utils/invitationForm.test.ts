import { describe, expect, test } from 'vitest';
import { ApiError, NetworkError } from '../services/apiError';
import { invitationEmailError, invitationSubmitError } from './invitationForm';

describe('I1 invitationEmailError', () => {
  test('accepts the email of someone else', () => {
    expect(invitationEmailError('friend@example.com', 'ana@example.com')).toBeUndefined();
  });

  test('asks for an email when it is empty or only spaces', () => {
    expect(invitationEmailError('', 'ana@example.com')).toBe('required');
    expect(invitationEmailError('   ', 'ana@example.com')).toBe('required');
  });

  test('refuses something that is not an email', () => {
    expect(invitationEmailError('not-an-email', 'ana@example.com')).toBe('invalid');
  });

  test("refuses the creator's own email whatever its case or the spaces around it", () => {
    expect(invitationEmailError(' Ana@Example.com ', 'ana@example.com')).toBe('own');
  });
});

describe('I1 invitationSubmitError', () => {
  test.each([
    [new ApiError(409), 'duplicate'],
    [new ApiError(400), 'invalidEmail'],
    [new ApiError(403), 'server'],
    [new ApiError(500), 'server'],
    [new NetworkError(), 'network'],
    [new Error('anything else'), 'server'],
  ])('%s is %s', (error, code) => {
    expect(invitationSubmitError(error)).toBe(code);
  });
});
