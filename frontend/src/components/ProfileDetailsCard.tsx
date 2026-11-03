import { useState, type SubmitEvent } from 'react';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Form from 'react-bootstrap/Form';
import { ApiError } from '../services/apiError';
import { updateProfile } from '../services/userService';
import type { User } from '../types';
import {
  MAX_NAME_LENGTH,
  emailError,
  nameError,
  type EmailError,
  type NameError,
} from '../utils/authForm';
import { formatMemberSince, profileErrorCode, type ProfileError } from '../utils/profileForm';

const EMAIL_MESSAGES: Record<EmailError, string> = {
  required: 'Escribe tu email.',
  invalid: 'Escribe un email válido, por ejemplo ana@correo.es.',
};

const NAME_MESSAGES: Record<NameError, string> = {
  required: 'Escribe tu nombre.',
  tooLong: `El nombre admite como máximo ${MAX_NAME_LENGTH} caracteres.`,
};

const PROFILE_MESSAGES: Record<ProfileError, string> = {
  sessionExpired: 'Tu sesión ha caducado. Inicia sesión de nuevo.',
  forbidden: 'No puedes modificar este perfil.',
  network: 'No se pudo conectar con el servidor. Inténtalo de nuevo.',
  invalidData: 'Revisa los datos del formulario',
  server: 'Algo ha fallado en el servidor. Inténtalo de nuevo más tarde.',
};

interface FieldMessages {
  name?: string;
  email?: string;
}

interface ProfileDetailsCardProps {
  profile: User;
  /** Called with the profile returned by the API after a successful save. */
  onSaved: (profile: User) => void;
}

/** Name, email and member-since date, with a view mode and an edit mode. */
export default function ProfileDetailsCard({
  profile,
  onSaved,
}: Readonly<ProfileDetailsCardProps>) {
  const isAdmin = profile.role === 'ADMIN';
  const [editing, setEditing] = useState(false);
  const [saved, setSaved] = useState(false);
  const [name, setName] = useState(profile.name);
  const [email, setEmail] = useState(profile.email);
  const [messages, setMessages] = useState<FieldMessages>({});
  const [submitError, setSubmitError] = useState<string>();
  const [submitting, setSubmitting] = useState(false);

  function startEditing() {
    setName(profile.name);
    setEmail(profile.email);
    setMessages({});
    setSubmitError(undefined);
    setSaved(false);
    setEditing(true);
  }

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    const nameCode = nameError(name);
    const emailCode = emailError(email);
    const found: FieldMessages = {
      name: nameCode && NAME_MESSAGES[nameCode],
      email: emailCode && EMAIL_MESSAGES[emailCode],
    };
    setMessages(found);
    setSubmitError(undefined);
    if (Object.values(found).some(Boolean)) return;

    setSubmitting(true);
    try {
      const updated = await updateProfile(profile.id, {
        name: name.trim(),
        email: email.trim(),
      });
      onSaved(updated);
      setEditing(false);
      setSaved(true);
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        setMessages({ email: 'Ya existe una cuenta con ese email.' });
      } else {
        setSubmitError(PROFILE_MESSAGES[profileErrorCode(error)]);
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card as="section" className="profile-card" aria-labelledby="profile-details-title">
      <h2 id="profile-details-title" className="profile-card__title">
        Tus datos
      </h2>
      {saved && (
        <Alert variant="success" role="status">
          Perfil actualizado.
        </Alert>
      )}
      {editing ? (
        <Form noValidate onSubmit={handleSubmit}>
          {submitError && <Alert variant="danger">{submitError}</Alert>}
          <Form.Group className="mb-3" controlId="profile-name">
            <Form.Label>Nombre</Form.Label>
            <Form.Control
              type="text"
              autoComplete="name"
              value={name}
              isInvalid={!!messages.name}
              onChange={(event) => setName(event.target.value)}
            />
            <Form.Control.Feedback type="invalid">{messages.name}</Form.Control.Feedback>
          </Form.Group>
          <Form.Group className="mb-4" controlId="profile-email">
            <Form.Label>Email</Form.Label>
            <Form.Control
              type="email"
              autoComplete="email"
              value={email}
              isInvalid={!!messages.email}
              onChange={(event) => setEmail(event.target.value)}
            />
            <Form.Control.Feedback type="invalid">{messages.email}</Form.Control.Feedback>
          </Form.Group>
          <div className="d-flex flex-wrap gap-2">
            <Button type="submit" disabled={submitting}>
              {submitting ? 'Guardando…' : 'Guardar cambios'}
            </Button>
            <Button
              variant="outline-secondary"
              disabled={submitting}
              onClick={() => setEditing(false)}
            >
              Cancelar
            </Button>
          </div>
        </Form>
      ) : (
        <>
          <dl className="profile-details">
            <dt>Nombre</dt>
            <dd>{profile.name}</dd>
            <dt>Email</dt>
            <dd>{profile.email}</dd>
            <dt>Miembro desde</dt>
            <dd>{formatMemberSince(profile.registrationDate)}</dd>
          </dl>
          {isAdmin ? (
            <p className="mb-0">
              Los datos del administrador se gestionan en la configuración de la aplicación.
            </p>
          ) : (
            <Button variant="outline-primary" onClick={startEditing}>
              Editar datos
            </Button>
          )}
        </>
      )}
    </Card>
  );
}
