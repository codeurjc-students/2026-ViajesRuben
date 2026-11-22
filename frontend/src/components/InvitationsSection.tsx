import { useCallback, useState, type SubmitEvent } from 'react';
import Alert from 'react-bootstrap/Alert';
import Badge from 'react-bootstrap/Badge';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Form from 'react-bootstrap/Form';
import { useAuth } from '../hooks/useAuth';
import { usePagedList } from '../hooks/usePagedList';
import { createInvitation, getInvitations } from '../services/invitationService';
import type { InvitationStatus } from '../types';
import {
  invitationEmailError,
  invitationSubmitError,
  type InvitationEmailError,
  type InvitationSubmitError,
} from '../utils/invitationForm';
import { formatTripDate } from '../utils/tripFormat';
import LoadMoreButton from './LoadMoreButton';
import './InvitationsSection.css';

/** How each state of an invitation is shown to the creator. */
const STATUS_BADGES: Record<InvitationStatus, { label: string; bg: string; text?: string }> = {
  PENDING: { label: 'Pendiente', bg: 'warning', text: 'dark' },
  ACCEPTED: { label: 'Aceptada', bg: 'success' },
  REJECTED: { label: 'Rechazada', bg: 'secondary' },
};

const EMAIL_MESSAGES: Record<InvitationEmailError, string> = {
  required: 'Escribe el email de la persona a la que quieres invitar.',
  invalid: 'Escribe un email válido, por ejemplo nombre@correo.com.',
  own: 'Revisa el email: no puede ser el tuyo.',
};

const SUBMIT_MESSAGES: Record<InvitationSubmitError, string> = {
  duplicate: 'Ya hay una invitación pendiente para ese email.',
  invalidEmail: 'Revisa el email.',
  network: 'No se pudo conectar con el servidor. Inténtalo de nuevo.',
  server: 'No se pudo enviar la invitación. Inténtalo de nuevo.',
};

/**
 * The creator's invitations of a trip: a form to invite an email and the list of what was sent,
 * newest first. The invited person gets an email with a link; answering it is I2.
 */
export default function InvitationsSection({ tripId }: Readonly<{ tripId: number }>) {
  const { user } = useAuth();
  const fetchPage = useCallback((page: number) => getInvitations(tripId, page), [tripId]);
  const { items, loaded, loading, last, error, loadMore, reload } = usePagedList(fetchPage);
  const [email, setEmail] = useState('');
  const [emailMessage, setEmailMessage] = useState<string>();
  const [submitError, setSubmitError] = useState<string>();
  const [sentTo, setSentTo] = useState<string>();
  const [sending, setSending] = useState(false);

  // Rethrown so the route's error page (inside the layout) is shown.
  if (error) throw error;

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitError(undefined);
    setSentTo(undefined);
    const problem = invitationEmailError(email, user?.email ?? '');
    setEmailMessage(problem && EMAIL_MESSAGES[problem]);
    if (problem) return;

    setSending(true);
    try {
      const invitation = await createInvitation(tripId, email.trim());
      setSentTo(invitation.invitedEmail);
      setEmail('');
      reload();
    } catch (failure) {
      setSubmitError(SUBMIT_MESSAGES[invitationSubmitError(failure)]);
    } finally {
      setSending(false);
    }
  }

  return (
    <Card as="section" className="invitations" aria-labelledby="invitations-title">
      <h2 id="invitations-title" className="h3 mb-3">
        Invitaciones
      </h2>
      <Form noValidate className="invitations__form" onSubmit={handleSubmit}>
        <Form.Group className="invitations__field" controlId="invitation-email">
          <Form.Label>Invitar por email</Form.Label>
          <Form.Control
            type="email"
            autoComplete="off"
            placeholder="nombre@correo.com"
            value={email}
            isInvalid={!!emailMessage}
            onChange={(event) => setEmail(event.target.value)}
          />
          <Form.Control.Feedback type="invalid">{emailMessage}</Form.Control.Feedback>
        </Form.Group>
        <Button type="submit" className="invitations__submit" disabled={sending}>
          {sending ? 'Enviando…' : 'Enviar invitación'}
        </Button>
      </Form>
      {submitError && (
        <Alert variant="danger" role="alert">
          {submitError}
        </Alert>
      )}
      {sentTo && (
        <Alert variant="success" role="status">
          Invitación enviada a {sentTo}.
        </Alert>
      )}
      {!loaded && <output className="d-block">Cargando invitaciones…</output>}
      {loaded && items.length === 0 && (
        <p className="invitations__empty">Aún no has invitado a nadie. Invita al primero.</p>
      )}
      {items.length > 0 && (
        <ul className="invitations__list">
          {items.map((invitation) => (
            <li key={invitation.id} className="invitations__item">
              <span className="invitations__email">{invitation.invitedEmail}</span>
              <span className="invitations__meta">
                <time dateTime={invitation.sentAt}>{formatTripDate(invitation.sentAt)}</time>
                <Badge
                  bg={STATUS_BADGES[invitation.status].bg}
                  text={STATUS_BADGES[invitation.status].text}
                >
                  {STATUS_BADGES[invitation.status].label}
                </Badge>
              </span>
            </li>
          ))}
        </ul>
      )}
      {loaded && <LoadMoreButton last={last} loading={loading} onClick={loadMore} />}
    </Card>
  );
}
