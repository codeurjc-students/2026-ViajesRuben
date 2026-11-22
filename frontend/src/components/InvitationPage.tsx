import { useEffect, useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Container from 'react-bootstrap/Container';
import { Link, useNavigate, useParams } from 'react-router';
import { ApiError } from '../services/apiError';
import { getInvitationByToken, respondToInvitation } from '../services/invitationService';
import type { InvitationAnswer, ReceivedInvitation } from '../types';
import './StatusPanel.css';

interface Loaded {
  token: string;
  invitation?: ReceivedInvitation;
  error?: unknown;
}

function loadErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 403) {
    return 'Esta invitación es para otra cuenta. Inicia sesión con el email al que se envió.';
  }
  if (error instanceof ApiError && error.status === 404) {
    return 'Esta invitación no existe o el viaje se ha eliminado.';
  }
  return 'No se pudo cargar la invitación. Inténtalo de nuevo.';
}

/**
 * `/invitations/:token`: the page the link of an invitation email opens, for a logged-in user (an
 * anonymous visitor is sent to log in and brought back). It shows the trip and who invites, and the
 * invited person accepts or rejects. Accepting goes to the trip. Whatever the state of the
 * invitation (answered, for another account, gone), the page says so. The token travels in the
 * path, so the page asks browsers not to send it on as a referrer.
 */
export default function InvitationPage() {
  const { token = '' } = useParams();
  const navigate = useNavigate();
  const [loaded, setLoaded] = useState<Loaded>();
  // Changes to load the invitation again, e.g. when it turns out to be answered already.
  const [attempt, setAttempt] = useState(0);
  const [sending, setSending] = useState(false);
  const [sendFailed, setSendFailed] = useState(false);

  useEffect(() => {
    let cancelled = false;
    getInvitationByToken(token)
      .then((invitation) => {
        if (!cancelled) setLoaded({ token, invitation });
      })
      .catch((error) => {
        if (!cancelled) setLoaded({ token, error });
      });
    return () => {
      cancelled = true;
    };
  }, [token, attempt]);

  // The answer is kept with its token, so a previous invitation is never shown for another link.
  const current = loaded?.token === token ? loaded : undefined;
  const invitation = current?.invitation;

  async function respond(answer: InvitationAnswer) {
    if (!invitation) return;
    setSending(true);
    setSendFailed(false);
    try {
      const answered = await respondToInvitation(invitation.id, answer);
      if (answer === 'ACCEPTED') {
        navigate(`/trips/${answered.tripId}`, {
          state: { notice: `Te has unido a «${answered.tripName}».` },
        });
        return;
      }
      setLoaded({ token, invitation: answered });
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        setAttempt((previous) => previous + 1);
      } else {
        setSendFailed(true);
      }
    }
    setSending(false);
  }

  let content;
  if (!current) {
    content = <output className="d-block">Cargando invitación…</output>;
  } else if (current.error) {
    content = (
      <>
        <Alert variant="warning" role="alert">
          {loadErrorMessage(current.error)}
        </Alert>
        <Link to="/trips">Ir a Mis viajes</Link>
      </>
    );
  } else if (invitation?.status === 'PENDING') {
    content = (
      <>
        <h1 className="h2">Invitación a un viaje</h1>
        <p>
          {invitation.inviterName} te invita a unirte a «{invitation.tripName}».
        </p>
        {sendFailed && (
          <Alert variant="danger" role="alert">
            No se pudo responder a la invitación. Inténtalo de nuevo.
          </Alert>
        )}
        <div className="d-flex flex-wrap gap-2">
          <Button disabled={sending} onClick={() => respond('ACCEPTED')}>
            Aceptar
          </Button>
          <Button
            variant="outline-secondary"
            disabled={sending}
            onClick={() => respond('REJECTED')}
          >
            Rechazar
          </Button>
        </div>
      </>
    );
  } else if (invitation?.status === 'ACCEPTED') {
    content = (
      <>
        <h1 className="h2">Invitación aceptada</h1>
        <p>Ya has aceptado la invitación a «{invitation.tripName}».</p>
        <Link to={`/trips/${invitation.tripId}`}>Ver viaje</Link>
      </>
    );
  } else if (invitation) {
    content = (
      <>
        <h1 className="h2">Invitación rechazada</h1>
        <p>Has rechazado la invitación a «{invitation.tripName}».</p>
        <Link to="/trips">Ir a Mis viajes</Link>
      </>
    );
  }

  return (
    <Container as="main" className="app-main py-4">
      <meta name="referrer" content="no-referrer" />
      <div className="status-panel">{content}</div>
    </Container>
  );
}
