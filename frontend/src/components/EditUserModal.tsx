import { useState, type SubmitEvent } from 'react';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import Modal from 'react-bootstrap/Modal';
import { ApiError } from '../services/apiError';
import { updateUser } from '../services/adminUserService';
import type { AdminUser } from '../types';
import { emailError, nameError } from '../utils/authForm';
import { profileErrorMessage } from '../utils/profileForm';

interface FieldMessages {
  name?: string;
  email?: string;
}

interface EditUserModalProps {
  user: AdminUser;
  onCancel: () => void;
  /** Called with the user as the API returns it after a successful save. */
  onSaved: (user: AdminUser) => void;
}

/** Form to change a user's name and email, with the same validation as the profile. */
export default function EditUserModal({ user, onCancel, onSaved }: Readonly<EditUserModalProps>) {
  const [name, setName] = useState(user.name);
  const [email, setEmail] = useState(user.email);
  const [messages, setMessages] = useState<FieldMessages>({});
  const [submitError, setSubmitError] = useState<string>();
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    const found: FieldMessages = { name: nameError(name), email: emailError(email) };
    setMessages(found);
    setSubmitError(undefined);
    if (Object.values(found).some(Boolean)) return;

    setSubmitting(true);
    try {
      onSaved(await updateUser(user.id, { name: name.trim(), email: email.trim() }));
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        setMessages({ email: 'Ya existe una cuenta con ese email.' });
      } else {
        setSubmitError(profileErrorMessage(error));
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal
      show
      onHide={onCancel}
      centered
      aria-labelledby="edit-user-title"
      backdrop={submitting ? 'static' : true}
      keyboard={!submitting}
    >
      <Form noValidate onSubmit={handleSubmit}>
        <Modal.Header closeButton={!submitting}>
          <Modal.Title as="h2" className="h4" id="edit-user-title">
            Editar usuario
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {submitError && <Alert variant="danger">{submitError}</Alert>}
          <Form.Group className="mb-3" controlId="edit-user-name">
            <Form.Label>Nombre</Form.Label>
            <Form.Control
              type="text"
              value={name}
              isInvalid={!!messages.name}
              onChange={(event) => setName(event.target.value)}
            />
            <Form.Control.Feedback type="invalid">{messages.name}</Form.Control.Feedback>
          </Form.Group>
          <Form.Group controlId="edit-user-email">
            <Form.Label>Email</Form.Label>
            <Form.Control
              type="email"
              value={email}
              isInvalid={!!messages.email}
              onChange={(event) => setEmail(event.target.value)}
            />
            <Form.Control.Feedback type="invalid">{messages.email}</Form.Control.Feedback>
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="outline-secondary" disabled={submitting} onClick={onCancel}>
            Cancelar
          </Button>
          <Button type="submit" disabled={submitting}>
            {submitting ? 'Guardando…' : 'Guardar cambios'}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}
