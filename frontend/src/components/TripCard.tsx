import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import type { Trip } from '../types';
import { tripDetails } from '../utils/sampleTripDetails';
import StoredImage from './StoredImage';
import './TripCard.css';

const dateFormat = new Intl.DateTimeFormat('es-ES', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  timeZone: 'UTC',
});

function formatDate(isoDate: string): string {
  return dateFormat.format(new Date(isoDate));
}

interface TripCardProps {
  trip: Trip;
}

export default function TripCard({ trip }: Readonly<TripCardProps>) {
  const title = `Viaje a ${trip.destination}`;
  const { participantCount, isCreator } = tripDetails(trip);

  return (
    <Card className="trip-card h-100">
      <div className="trip-card__image">
        <StoredImage />
      </div>
      <div className="trip-card__info">
        <Card.Title as="h2" className="h4">
          {title}
        </Card.Title>
        <p className="trip-card__meta">
          {`Fechas: ${formatDate(trip.startDate)} – ${formatDate(trip.endDate)}`}
        </p>
        <p className="trip-card__meta">
          <svg viewBox="0 0 24 24" aria-hidden="true" className="trip-card__icon">
            <circle cx="9" cy="8" r="3" />
            <path d="M3 19c0-3.3 2.7-5 6-5s6 1.7 6 5M16 5.2a3 3 0 0 1 0 5.6M18 14.3c1.8.7 3 2.2 3 4.7" />
          </svg>
          {participantCount === 1 ? '1 participante' : `${participantCount} participantes`}
        </p>
        <div className="trip-card__actions">
          <Button variant="outline-danger" size="sm" aria-label={`Eliminar ${title}`}>
            <svg viewBox="0 0 24 24" aria-hidden="true" className="trip-card__icon">
              <path d="M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3" />
            </svg>
          </Button>
          {isCreator && (
            <Button variant="outline-primary" size="sm" aria-label={`Editar ${title}`}>
              <svg viewBox="0 0 24 24" aria-hidden="true" className="trip-card__icon">
                <path d="M4 20h4L19 9l-4-4L4 16ZM13 7l4 4" />
              </svg>
            </Button>
          )}
        </div>
      </div>
    </Card>
  );
}
