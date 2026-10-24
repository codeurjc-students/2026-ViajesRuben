import { describe, expect, test } from 'vitest';
import { ApiError, NetworkError } from '../services/apiError';
import { formatMemberSince, profileErrorMessage, validatePasswordChange } from './profileForm';

describe('B3 profileForm', () => {
  test('profileErrorMessage explains 401 and 403 for a profile, not for a login', () => {
    expect(profileErrorMessage(new ApiError(401))).toBe(
      'Tu sesión ha caducado. Inicia sesión de nuevo.',
    );
    expect(profileErrorMessage(new ApiError(403))).toBe('No puedes modificar este perfil.');
  });

  test('profileErrorMessage leaves the other failures to the shared messages', () => {
    expect(profileErrorMessage(new ApiError(400))).toBe('Revisa los datos del formulario');
    expect(profileErrorMessage(new NetworkError())).toMatch(/No se pudo conectar/);
    expect(profileErrorMessage(new ApiError(500))).toMatch(/Algo ha fallado/);
  });

  test('validatePasswordChange accepts a complete, matching form', () => {
    expect(validatePasswordChange('old-secret', 'new-secret', 'new-secret')).toEqual({});
  });

  test('validatePasswordChange asks for every empty field', () => {
    expect(validatePasswordChange('', '', '')).toEqual({
      current: 'Escribe tu contraseña actual.',
      next: 'Escribe la nueva contraseña.',
      confirmation: 'Repite la nueva contraseña.',
    });
  });

  test('validatePasswordChange keeps the new password between 8 and 72 characters', () => {
    expect(validatePasswordChange('old', 'short', 'short').next).toMatch(/al menos 8/);
    expect(validatePasswordChange('old', 'x'.repeat(8), 'x'.repeat(8)).next).toBeUndefined();
    expect(validatePasswordChange('old', 'x'.repeat(72), 'x'.repeat(72)).next).toBeUndefined();
    expect(validatePasswordChange('old', 'x'.repeat(73), 'x'.repeat(73)).next).toMatch(
      /como máximo 72/,
    );
  });

  test('validatePasswordChange detects a confirmation that does not match', () => {
    expect(validatePasswordChange('old', 'new-secret', 'other-secret').confirmation).toBe(
      'Las contraseñas no coinciden.',
    );
  });

  test('formatMemberSince writes the date in Spanish without shifting the day', () => {
    expect(formatMemberSince('2026-10-01')).toBe('1 de octubre de 2026');
    expect(formatMemberSince('2027-01-31')).toBe('31 de enero de 2027');
  });
});
