import { useState, type SubmitEvent } from 'react';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Container from 'react-bootstrap/Container';
import Form from 'react-bootstrap/Form';
import { Link, Navigate, useLocation } from 'react-router';
import { useAuth } from '../hooks/useAuth';
import { ApiError } from '../services/apiError';
import {
  MIN_PASSWORD_LENGTH,
  destinationAfterLogin,
  emailError,
  submitErrorMessage,
} from '../utils/authForm';
import './AuthPages.css';

interface FieldMessages {
  name?: string;
  email?: string;
  password?: string;
  confirmation?: string;
}

const MAX_NAME_LENGTH = 100;

function validate(name: string, email: string, password: string, confirmation: string) {
  const messages: FieldMessages = {};
  if (!name.trim()) messages.name = 'Escribe tu nombre.';
  else if (name.trim().length > MAX_NAME_LENGTH)
    messages.name = `El nombre admite como máximo ${MAX_NAME_LENGTH} caracteres.`;
  messages.email = emailError(email);
  if (!password) messages.password = 'Escribe una contraseña.';
  else if (password.length < MIN_PASSWORD_LENGTH)
    messages.password = `La contraseña necesita al menos ${MIN_PASSWORD_LENGTH} caracteres.`;
  if (!confirmation) messages.confirmation = 'Repite la contraseña.';
  else if (confirmation !== password) messages.confirmation = 'Las contraseñas no coinciden.';
  return messages;
}

export default function RegisterPage() {
  const { user, register } = useAuth();
  const location = useLocation();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [messages, setMessages] = useState<FieldMessages>({});
  const [submitError, setSubmitError] = useState<string>();
  const [submitting, setSubmitting] = useState(false);

  // Already logged in (also right after registering): go where the visitor was heading.
  if (user) return <Navigate to={destinationAfterLogin(location.state)} replace />;

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    const found = validate(name, email, password, confirmation);
    setMessages(found);
    setSubmitError(undefined);
    if (Object.values(found).some(Boolean)) return;

    setSubmitting(true);
    try {
      await register({ name: name.trim(), email: email.trim(), password });
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        setMessages({ email: 'Ya existe una cuenta con ese email.' });
      } else {
        setSubmitError(submitErrorMessage(error));
      }
      setSubmitting(false);
    }
  }

  return (
    <Container as="main" className="app-main py-4">
      <div className="auth-page">
        <h1 className="auth-page__title mb-4 text-white">Crear cuenta</h1>
        <Card className="auth-card">
          <Form noValidate onSubmit={handleSubmit}>
            {submitError && <Alert variant="danger">{submitError}</Alert>}
            <Form.Group className="mb-3" controlId="register-name">
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
            <Form.Group className="mb-3" controlId="register-email">
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
            <Form.Group className="mb-3" controlId="register-password">
              <Form.Label>Contraseña</Form.Label>
              <Form.Control
                type="password"
                autoComplete="new-password"
                value={password}
                isInvalid={!!messages.password}
                onChange={(event) => setPassword(event.target.value)}
              />
              <Form.Text>Al menos {MIN_PASSWORD_LENGTH} caracteres.</Form.Text>
              <Form.Control.Feedback type="invalid">{messages.password}</Form.Control.Feedback>
            </Form.Group>
            <Form.Group className="mb-4" controlId="register-confirmation">
              <Form.Label>Confirmar contraseña</Form.Label>
              <Form.Control
                type="password"
                autoComplete="new-password"
                value={confirmation}
                isInvalid={!!messages.confirmation}
                onChange={(event) => setConfirmation(event.target.value)}
              />
              <Form.Control.Feedback type="invalid">{messages.confirmation}</Form.Control.Feedback>
            </Form.Group>
            <Button type="submit" size="lg" className="w-100" disabled={submitting}>
              {submitting ? 'Creando cuenta…' : 'Crear cuenta'}
            </Button>
          </Form>
          <p className="auth-card__switch">
            ¿Ya tienes cuenta?{' '}
            <Link to="/login" state={location.state}>
              Inicia sesión
            </Link>
          </p>
        </Card>
      </div>
    </Container>
  );
}
