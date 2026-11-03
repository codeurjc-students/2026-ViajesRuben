import { useState, type SubmitEvent } from 'react';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Form from 'react-bootstrap/Form';
import { ApiError } from '../services/apiError';
import { changePassword } from '../services/userService';
import { MAX_PASSWORD_LENGTH, MIN_PASSWORD_LENGTH } from '../utils/authForm';
import {
  profileErrorCode,
  validatePasswordChange,
  type PasswordFieldErrors,
  type ProfileError,
} from '../utils/profileForm';

interface FieldMessages {
  current?: string;
  next?: string;
  confirmation?: string;
}

const PASSWORD_MESSAGES = {
  current: { required: 'Escribe tu contraseña actual.' },
  next: {
    required: 'Escribe la nueva contraseña.',
    tooShort: `La contraseña necesita al menos ${MIN_PASSWORD_LENGTH} caracteres.`,
    tooLong: `La contraseña admite como máximo ${MAX_PASSWORD_LENGTH} caracteres.`,
  },
  confirmation: {
    required: 'Repite la nueva contraseña.',
    mismatch: 'Las contraseñas no coinciden.',
  },
};

const PROFILE_MESSAGES: Record<ProfileError, string> = {
  sessionExpired: 'Tu sesión ha caducado. Inicia sesión de nuevo.',
  forbidden: 'No puedes modificar este perfil.',
  network: 'No se pudo conectar con el servidor. Inténtalo de nuevo.',
  invalidData: 'Revisa los datos del formulario',
  server: 'Algo ha fallado en el servidor. Inténtalo de nuevo más tarde.',
};

function passwordMessages(errors: PasswordFieldErrors): FieldMessages {
  return {
    current: errors.current && PASSWORD_MESSAGES.current[errors.current],
    next: errors.next && PASSWORD_MESSAGES.next[errors.next],
    confirmation: errors.confirmation && PASSWORD_MESSAGES.confirmation[errors.confirmation],
  };
}

/** Separate form to change the password: the current one, the new one and its confirmation. */
export default function PasswordCard({ userId }: Readonly<{ userId: number }>) {
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [messages, setMessages] = useState<FieldMessages>({});
  const [submitError, setSubmitError] = useState<string>();
  const [changed, setChanged] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    const found = passwordMessages(validatePasswordChange(current, next, confirmation));
    setMessages(found);
    setSubmitError(undefined);
    setChanged(false);
    if (Object.values(found).some(Boolean)) return;

    setSubmitting(true);
    try {
      await changePassword(userId, { currentPassword: current, newPassword: next });
      setCurrent('');
      setNext('');
      setConfirmation('');
      setChanged(true);
    } catch (error) {
      if (error instanceof ApiError && error.status === 400) {
        // The new password was already checked here, so the server can only be refusing the current one.
        setMessages({ current: 'La contraseña actual no es correcta.' });
      } else {
        setSubmitError(PROFILE_MESSAGES[profileErrorCode(error)]);
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card as="section" className="profile-card" aria-labelledby="profile-password-title">
      <h2 id="profile-password-title" className="profile-card__title">
        Cambiar contraseña
      </h2>
      <Form noValidate onSubmit={handleSubmit}>
        {changed && (
          <Alert variant="success" role="status">
            Contraseña cambiada. Úsala la próxima vez que inicies sesión.
          </Alert>
        )}
        {submitError && <Alert variant="danger">{submitError}</Alert>}
        <Form.Group className="mb-3" controlId="profile-current-password">
          <Form.Label>Contraseña actual</Form.Label>
          <Form.Control
            type="password"
            autoComplete="current-password"
            value={current}
            isInvalid={!!messages.current}
            onChange={(event) => setCurrent(event.target.value)}
          />
          <Form.Control.Feedback type="invalid">{messages.current}</Form.Control.Feedback>
        </Form.Group>
        <Form.Group className="mb-3" controlId="profile-new-password">
          <Form.Label>Nueva contraseña</Form.Label>
          <Form.Control
            type="password"
            autoComplete="new-password"
            value={next}
            isInvalid={!!messages.next}
            onChange={(event) => setNext(event.target.value)}
          />
          <Form.Text>Al menos {MIN_PASSWORD_LENGTH} caracteres.</Form.Text>
          <Form.Control.Feedback type="invalid">{messages.next}</Form.Control.Feedback>
        </Form.Group>
        <Form.Group className="mb-4" controlId="profile-confirm-password">
          <Form.Label>Confirmar nueva contraseña</Form.Label>
          <Form.Control
            type="password"
            autoComplete="new-password"
            value={confirmation}
            isInvalid={!!messages.confirmation}
            onChange={(event) => setConfirmation(event.target.value)}
          />
          <Form.Control.Feedback type="invalid">{messages.confirmation}</Form.Control.Feedback>
        </Form.Group>
        <Button type="submit" disabled={submitting}>
          {submitting ? 'Cambiando…' : 'Guardar contraseña'}
        </Button>
      </Form>
    </Card>
  );
}
