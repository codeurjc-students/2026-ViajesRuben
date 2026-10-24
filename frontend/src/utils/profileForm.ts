import { ApiError } from '../services/apiError';
import { MAX_PASSWORD_LENGTH, MIN_PASSWORD_LENGTH, submitErrorMessage } from './authForm';

export interface PasswordFieldMessages {
  current?: string;
  next?: string;
  confirmation?: string;
}

/** Message for a failed profile request; 401 and 403 mean something else here than at login. */
export function profileErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 401) return 'Tu sesión ha caducado. Inicia sesión de nuevo.';
    if (error.status === 403) return 'No puedes modificar este perfil.';
  }
  return submitErrorMessage(error);
}

export function validatePasswordChange(
  current: string,
  next: string,
  confirmation: string,
): PasswordFieldMessages {
  const messages: PasswordFieldMessages = {};
  if (!current) messages.current = 'Escribe tu contraseña actual.';
  if (!next) messages.next = 'Escribe la nueva contraseña.';
  else if (next.length < MIN_PASSWORD_LENGTH)
    messages.next = `La contraseña necesita al menos ${MIN_PASSWORD_LENGTH} caracteres.`;
  else if (next.length > MAX_PASSWORD_LENGTH)
    messages.next = `La contraseña admite como máximo ${MAX_PASSWORD_LENGTH} caracteres.`;
  if (!confirmation) messages.confirmation = 'Repite la nueva contraseña.';
  else if (confirmation !== next) messages.confirmation = 'Las contraseñas no coinciden.';
  return messages;
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
