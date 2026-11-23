import { useCallback, useState } from 'react';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import { useAuth } from '../hooks/useAuth';
import { usePagedList } from '../hooks/usePagedList';
import { getParticipants } from '../services/participantService';
import type { Participant } from '../types';
import { formatTripDate } from '../utils/tripFormat';
import LoadMoreButton from './LoadMoreButton';
import RemoveParticipantModal from './RemoveParticipantModal';
import UserAvatar from './UserAvatar';
import './ParticipantsSection.css';

interface ParticipantsSectionProps {
  tripId: number;
  /** The creator removes anyone; a participant only reads the list. */
  canRemove: boolean;
}

/**
 * The people who joined the trip, oldest first (I3). Everyone who can see the trip sees their
 * names and avatars; only the creator receives their emails and can remove them.
 */
export default function ParticipantsSection({
  tripId,
  canRemove,
}: Readonly<ParticipantsSectionProps>) {
  const { user } = useAuth();
  const fetchPage = useCallback((page: number) => getParticipants(tripId, page), [tripId]);
  const { items, loaded, loading, last, error, loadMore, reload } = usePagedList(fetchPage);
  const [removing, setRemoving] = useState<Participant>();

  // Rethrown so the route's error page (inside the layout) is shown.
  if (error) throw error;

  function removed() {
    setRemoving(undefined);
    reload();
  }

  return (
    <Card as="section" className="participants" aria-labelledby="participants-title">
      <h2 id="participants-title" className="h3 mb-3">
        Participantes
      </h2>
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
