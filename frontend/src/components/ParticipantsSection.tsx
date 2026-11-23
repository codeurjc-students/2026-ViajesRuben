import { useCallback, useState } from 'react';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import { useNavigate } from 'react-router';
import { useAuth } from '../hooks/useAuth';
import { usePagedList } from '../hooks/usePagedList';
import { getParticipants } from '../services/participantService';
import type { Participant } from '../types';
import { formatTripDate } from '../utils/tripFormat';
import LeaveTripModal from './LeaveTripModal';
import LoadMoreButton from './LoadMoreButton';
import RemoveParticipantModal from './RemoveParticipantModal';
import UserAvatar from './UserAvatar';
import './ParticipantsSection.css';

interface ParticipantsSectionProps {
  tripId: number;
  tripName: string;
  /**
   * The creator removes anyone and sees their emails. Anyone else is a participant: they read the
   * list and can leave the trip.
   */
  canRemove: boolean;
}

/**
 * The people who joined the trip, oldest first (I3). Everyone who can see the trip sees their
 * names and avatars; only the creator receives their emails and can remove them, and a
 * participant can leave, which takes them back to "Mis viajes" with a notice.
 */
export default function ParticipantsSection({
  tripId,
  tripName,
  canRemove,
}: Readonly<ParticipantsSectionProps>) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const fetchPage = useCallback((page: number) => getParticipants(tripId, page), [tripId]);
  const { items, loaded, loading, last, error, loadMore, reload } = usePagedList(fetchPage);
  const [removing, setRemoving] = useState<Participant>();
  const [leaving, setLeaving] = useState(false);

  // Rethrown so the route's error page (inside the layout) is shown.
  if (error) throw error;

  function removed() {
    setRemoving(undefined);
    reload();
  }

  function left() {
    navigate('/trips', { state: { notice: `Has salido de «${tripName}».` } });
  }

  return (
    <Card as="section" className="participants" aria-labelledby="participants-title">
      <div className="participants__header">
        <h2 id="participants-title" className="h3 mb-0">
          Participantes
        </h2>
        {!canRemove && user && (
          <Button variant="outline-danger" size="sm" onClick={() => setLeaving(true)}>
            Salir del viaje
          </Button>
        )}
      </div>
      {!loaded && <output className="d-block">Cargando participantes…</output>}
      {loaded && items.length === 0 && (
        <p className="participants__empty">
          {canRemove
            ? 'Aún no se ha unido nadie. Invita a alguien desde Invitaciones.'
            : 'Aún no se ha unido nadie.'}
        </p>
      )}
      {items.length > 0 && (
        <ul className="participants__list">
          {items.map((participant) => (
            <li key={participant.userId} className="participants__item">
              <UserAvatar user={participant} />
              <span className="participants__who">
                <span className="participants__name">
                  {participant.name}
                  {participant.userId === user?.id && ' (tú)'}
                </span>
                {participant.email && (
                  <span className="participants__email">{participant.email}</span>
                )}
              </span>
              <span className="participants__meta">
                Se unió el{' '}
                <time dateTime={participant.joinedAt}>{formatTripDate(participant.joinedAt)}</time>
              </span>
              {canRemove && (
                <Button variant="outline-danger" size="sm" onClick={() => setRemoving(participant)}>
                  Quitar <span className="visually-hidden">a {participant.name}</span>
                </Button>
              )}
            </li>
          ))}
        </ul>
      )}
      {loaded && <LoadMoreButton last={last} loading={loading} onClick={loadMore} />}
      {leaving && user && (
        <LeaveTripModal
          tripId={tripId}
          tripName={tripName}
          userId={user.id}
          onCancel={() => setLeaving(false)}
          onLeft={left}
        />
      )}
      {removing && (
        <RemoveParticipantModal
          tripId={tripId}
          participant={removing}
          onCancel={() => setRemoving(undefined)}
          onRemoved={removed}
        />
      )}
    </Card>
  );
}
