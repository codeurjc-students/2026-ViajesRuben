import { useState, type SubmitEvent } from 'react';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Form from 'react-bootstrap/Form';
import { ApiError } from '../services/apiError';
import { changePassword } from '../services/userService';
import { MIN_PASSWORD_LENGTH } from '../utils/authForm';
import {
  profileErrorMessage,
  validatePasswordChange,
  type PasswordFieldMessages,
} from '../utils/profileForm';

/** Separate form to change the password: the current one, the new one and its confirmation. */
export default function PasswordCard({ userId }: Readonly<{ userId: number }>) {
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [messages, setMessages] = useState<PasswordFieldMessages>({});
  const [submitError, setSubmitError] = useState<string>();
  const [changed, setChanged] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    const found = validatePasswordChange(current, next, confirmation);
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
        setSubmitError(profileErrorMessage(error));
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
