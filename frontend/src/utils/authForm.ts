import type { Location } from 'react-router';
import { ApiError, NetworkError } from '../services/apiError';

export const MIN_PASSWORD_LENGTH = 8;
/** BCrypt only reads the first 72 bytes, so the backend refuses longer passwords. */
export const MAX_PASSWORD_LENGTH = 72;
export const MAX_NAME_LENGTH = 100;

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function emailError(email: string): string | undefined {
  if (!email.trim()) return 'Escribe tu email.';
  if (!EMAIL_PATTERN.test(email.trim()))
    return 'Escribe un email válido, por ejemplo ana@correo.es.';
  return undefined;
}

export function nameError(name: string): string | undefined {
  if (!name.trim()) return 'Escribe tu nombre.';
  if (name.trim().length > MAX_NAME_LENGTH)
    return `El nombre admite como máximo ${MAX_NAME_LENGTH} caracteres.`;
  return undefined;
}

/** Message for a failure that does not belong to a single field; the API texts are English. */
export function submitErrorMessage(error: unknown): string {
  if (error instanceof NetworkError)
    return 'No se pudo conectar con el servidor. Inténtalo de nuevo.';
  if (error instanceof ApiError) {
    if (error.status === 400) return 'Revisa los datos del formulario';
    if (error.status === 401) return 'Email o contraseña incorrectos';
    if (error.status === 403) return 'Tu cuenta está bloqueada';
  }
  return 'Algo ha fallado en el servidor. Inténtalo de nuevo más tarde.';
}

/** The page a visitor was sent away from by RequireAuth, or the trip list. */
export function destinationAfterLogin(state: unknown): string {
  const from = (state as { from?: Location } | null)?.from;
  return from ? `${from.pathname}${from.search}${from.hash}` : '/trips';
}
