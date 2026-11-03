import { useState, type SubmitEvent } from 'react';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Container from 'react-bootstrap/Container';
import Form from 'react-bootstrap/Form';
import { Link, Navigate, useLocation } from 'react-router';
import { useAuth } from '../hooks/useAuth';
import {
  destinationAfterLogin,
  emailError,
  submitErrorCode,
  type EmailError,
  type SubmitError,
} from '../utils/authForm';
import './AuthPages.css';

const EMAIL_MESSAGES: Record<EmailError, string> = {
  required: 'Escribe tu email.',
  invalid: 'Escribe un email válido, por ejemplo ana@correo.es.',
};

const SUBMIT_MESSAGES: Record<SubmitError, string> = {
  network: 'No se pudo conectar con el servidor. Inténtalo de nuevo.',
  invalidData: 'Revisa los datos del formulario',
  badCredentials: 'Email o contraseña incorrectos',
  blocked: 'Tu cuenta está bloqueada',
  server: 'Algo ha fallado en el servidor. Inténtalo de nuevo más tarde.',
};

export default function LoginPage() {
  const { user, login } = useAuth();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [emailMessage, setEmailMessage] = useState<string>();
  const [passwordMessage, setPasswordMessage] = useState<string>();
  const [submitError, setSubmitError] = useState<string>();
  const [submitting, setSubmitting] = useState(false);

  // Already logged in (also right after logging in): go where the visitor was heading.
  if (user) return <Navigate to={destinationAfterLogin(location.state)} replace />;

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    const emailCode = emailError(email);
    const nextEmailMessage = emailCode && EMAIL_MESSAGES[emailCode];
    const nextPasswordMessage = password ? undefined : 'Escribe tu contraseña.';
    setEmailMessage(nextEmailMessage);
    setPasswordMessage(nextPasswordMessage);
    setSubmitError(undefined);
    if (nextEmailMessage || nextPasswordMessage) return;

    setSubmitting(true);
    try {
      await login({ email: email.trim(), password });
    } catch (error) {
      setSubmitError(SUBMIT_MESSAGES[submitErrorCode(error)]);
      setSubmitting(false);
    }
  }

  return (
    <Container as="main" className="app-main py-4">
      <div className="auth-page">
        <h1 className="auth-page__title mb-4 text-white">Iniciar sesión</h1>
        <Card className="auth-card">
          <Form noValidate onSubmit={handleSubmit}>
            {submitError && <Alert variant="danger">{submitError}</Alert>}
            <Form.Group className="mb-3" controlId="login-email">
              <Form.Label>Email</Form.Label>
              <Form.Control
                type="email"
                autoComplete="email"
                value={email}
                isInvalid={!!emailMessage}
                onChange={(event) => setEmail(event.target.value)}
              />
              <Form.Control.Feedback type="invalid">{emailMessage}</Form.Control.Feedback>
            </Form.Group>
            <Form.Group className="mb-4" controlId="login-password">
              <Form.Label>Contraseña</Form.Label>
              <Form.Control
                type="password"
                autoComplete="current-password"
                value={password}
                isInvalid={!!passwordMessage}
                onChange={(event) => setPassword(event.target.value)}
              />
              <Form.Control.Feedback type="invalid">{passwordMessage}</Form.Control.Feedback>
            </Form.Group>
            <Button type="submit" size="lg" className="w-100" disabled={submitting}>
              {submitting ? 'Entrando…' : 'Iniciar sesión'}
            </Button>
          </Form>
          <p className="auth-card__switch">
            ¿No tienes cuenta?{' '}
            <Link to="/register" state={location.state}>
              Regístrate
            </Link>
          </p>
        </Card>
      </div>
    </Container>
  );
}
