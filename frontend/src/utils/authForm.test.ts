import { test, expect, describe } from 'vitest';
import { ApiError, NetworkError } from '../services/apiError';
import { destinationAfterLogin, emailError, nameError, submitErrorMessage } from './authForm';

describe('B2 authForm', () => {
  test('emailError accepts a normal address and explains the problem otherwise', () => {
    expect(emailError('ana@example.com')).toBeUndefined();
    expect(emailError('   ')).toBe('Escribe tu email.');
    expect(emailError('ana@')).toMatch(/email válido/);
  });

  test('submitErrorMessage maps each failure to a Spanish text', () => {
    expect(submitErrorMessage(new ApiError(401))).toBe('Email o contraseña incorrectos');
    expect(submitErrorMessage(new ApiError(403))).toBe('Tu cuenta está bloqueada');
    expect(submitErrorMessage(new ApiError(400))).toBe('Revisa los datos del formulario');
    expect(submitErrorMessage(new NetworkError())).toMatch(/No se pudo conectar/);
    expect(submitErrorMessage(new ApiError(500))).toMatch(/Algo ha fallado/);
    expect(submitErrorMessage(new Error('x'))).toMatch(/Algo ha fallado/);
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
    expect(nameError('   ')).toBe('Escribe tu nombre.');
    expect(nameError('x'.repeat(101))).toMatch(/como máximo 100/);
  });
});
