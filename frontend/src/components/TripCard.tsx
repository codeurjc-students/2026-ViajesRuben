import { useState } from 'react';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import { Link } from 'react-router';
import { useAuth } from '../hooks/useAuth';
import type { Trip } from '../types';
import { formatDateRange } from '../utils/tripFormat';
import DeleteTripModal from './DeleteTripModal';
import StoredImage from './StoredImage';
import './TripCard.css';

interface TripCardProps {
  trip: Trip;
  /** Called after the trip was deleted from the card, so the list can reload. */
  onDeleted: () => void;
}

export default function TripCard({ trip, onDeleted }: Readonly<TripCardProps>) {
  const { user } = useAuth();
  const [confirming, setConfirming] = useState(false);
  const isCreator = user?.id === trip.creatorId;

  return (
    <Card className="trip-card h-100">
      <div className="trip-card__image">
        <StoredImage id={trip.coverImageId ?? undefined} />
      </div>
      <div className="trip-card__info">
        <Card.Title as="h2" className="h4">
          <Link to={`/trips/${trip.id}`} className="trip-card__link">
            {trip.name}
          </Link>
        </Card.Title>
        <p className="trip-card__meta">{`Destino: ${trip.destination}`}</p>
        <p className="trip-card__meta">{`Fechas: ${formatDateRange(trip.startDate, trip.endDate)}`}</p>
        {isCreator && (
          <div className="trip-card__actions">
            <Button
              variant="outline-danger"
              size="sm"
              aria-label={`Eliminar ${trip.name}`}
              onClick={() => setConfirming(true)}
            >
              <svg viewBox="0 0 24 24" aria-hidden="true" className="trip-card__icon">
                <path d="M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3" />
              </svg>
            </Button>
            <Link
              to={`/trips/${trip.id}/edit`}
              className="btn btn-outline-primary btn-sm"
              aria-label={`Editar ${trip.name}`}
            >
              <svg viewBox="0 0 24 24" aria-hidden="true" className="trip-card__icon">
                <path d="M4 20h4L19 9l-4-4L4 16ZM13 7l4 4" />
              </svg>
            </Link>
          </div>
        )}
      </div>
      {isCreator && (
        <DeleteTripModal
          trip={trip}
          show={confirming}
          onCancel={() => setConfirming(false)}
          onDeleted={() => {
            setConfirming(false);
            onDeleted();
          }}
        />
      )}
    </Card>
  );
}
