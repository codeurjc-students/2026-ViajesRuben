import { ApiError, NetworkError } from '../services/apiError';
import { emailError } from './authForm';

/** Why the email of the invitation form is wrong; the component turns each code into Spanish. */
export type InvitationEmailError = 'required' | 'invalid' | 'own';

/** Why sending an invitation failed on the server side or on the way there. */
export type InvitationSubmitError = 'duplicate' | 'invalidEmail' | 'network' | 'server';

/** Checks the email before sending; `ownEmail` is the creator's, who cannot invite themselves. */
export function invitationEmailError(
  email: string,
  ownEmail: string,
): InvitationEmailError | undefined {
  const problem = emailError(email);
  if (problem) return problem;
  return email.trim().toLowerCase() === ownEmail.toLowerCase() ? 'own' : undefined;
}

/** Code for a failed request: 409 is a pending invitation that already exists, 400 a bad email. */
export function invitationSubmitError(error: unknown): InvitationSubmitError {
  if (error instanceof NetworkError) return 'network';
  if (error instanceof ApiError) {
    if (error.status === 409) return 'duplicate';
    if (error.status === 400) return 'invalidEmail';
  }
  return 'server';
}
