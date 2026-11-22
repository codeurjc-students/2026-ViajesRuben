import { useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import { usePagedList } from '../hooks/usePagedList';
import { ApiError } from '../services/apiError';
import { getMyInvitations, respondToInvitation } from '../services/invitationService';
import type { InvitationAnswer, ReceivedInvitation } from '../types';
import { formatTripDate } from '../utils/tripFormat';
import LoadMoreButton from './LoadMoreButton';
import './PendingInvitations.css';

interface PendingInvitationsProps {
  /** Called after an invitation was accepted, so "Mis viajes" can reload and show the new trip. */
  onAccepted: () => void;
}

/**
 * The invitations to a trip that wait for the logged-in user's answer (I2), above their trips in
 * "Mis viajes". Nothing is shown while there are none. An answered invitation leaves the list at
 * once, without loading it again.
 */
export default function PendingInvitations({ onAccepted }: Readonly<PendingInvitationsProps>) {
  const { items, loaded, loading, last, error, loadMore } = usePagedList(getMyInvitations);
  // The invitations answered from this list: they are hidden without asking the server again.
  const [answered, setAnswered] = useState<ReadonlySet<number>>(new Set());
  const [busy, setBusy] = useState<number>();
  const [failed, setFailed] = useState(false);
  const [notice, setNotice] = useState<string>();

  const pending = items.filter((invitation) => !answered.has(invitation.id));

  async function respond(invitation: ReceivedInvitation, answer: InvitationAnswer) {
    setBusy(invitation.id);
    setFailed(false);
    setNotice(undefined);
    try {
      await respondToInvitation(invitation.id, answer);
      setAnswered((previous) => new Set(previous).add(invitation.id));
      if (answer === 'ACCEPTED') {
        setNotice(`Te has unido a «${invitation.tripName}».`);
        onAccepted();
      }
    } catch (respondError) {
      if (respondError instanceof ApiError && respondError.status === 409) {
        // Answered meanwhile, e.g. from the email link: it is not pending any more.
        setAnswered((previous) => new Set(previous).add(invitation.id));
      } else {
        setFailed(true);
      }
    } finally {
      setBusy(undefined);
    }
  }

  return (
    <>
      {notice && (
        <Alert variant="success" role="status" dismissible onClose={() => setNotice(undefined)}>
          {notice}
        </Alert>
      )}
      {error !== null && (
        <Alert variant="warning" role="alert">
          No se pudieron cargar tus invitaciones pendientes. Recarga la página para intentarlo de
          nuevo.
        </Alert>
      )}
      {failed && (
        <Alert variant="danger" role="alert">
          No se pudo responder a la invitación. Inténtalo de nuevo.
        </Alert>
      )}
      {loaded && (pending.length > 0 || !last) && (
        <Card as="section" className="pending-invitations" aria-labelledby="pending-invitations">
          <h2 id="pending-invitations" className="h4">
            Invitaciones pendientes
          </h2>
          <ul className="pending-invitations__list">
            {pending.map((invitation) => (
              <li key={invitation.id} className="pending-invitations__item">
                <span className="pending-invitations__text">
                  {invitation.inviterName} te invita a «{invitation.tripName}»
                  <time dateTime={invitation.sentAt} className="pending-invitations__date">
                    {formatTripDate(invitation.sentAt)}
                  </time>
                </span>
                <span className="pending-invitations__actions">
                  <Button
                    size="sm"
                    disabled={busy !== undefined}
                    aria-label={`Aceptar invitación a ${invitation.tripName}`}
                    onClick={() => respond(invitation, 'ACCEPTED')}
                  >
                    Aceptar
                  </Button>
                  <Button
                    size="sm"
                    variant="outline-secondary"
                    disabled={busy !== undefined}
                    aria-label={`Rechazar invitación a ${invitation.tripName}`}
                    onClick={() => respond(invitation, 'REJECTED')}
                  >
                    Rechazar
                  </Button>
                </span>
              </li>
            ))}
          </ul>
          <LoadMoreButton last={last} loading={loading} onClick={loadMore} />
        </Card>
      )}
    </>
  );
}
