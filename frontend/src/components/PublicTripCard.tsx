import Card from 'react-bootstrap/Card';
import { Link } from 'react-router';
import type { PublicTripSummary } from '../types';
import { tripDays } from '../utils/tripDays';
import { formatBudget } from '../utils/tripFormat';
import StoredImage from './StoredImage';
import './PublicTripCard.css';

interface PublicTripCardProps {
  summary: PublicTripSummary;
}

/** A public itinerary on the home page: cover, key facts, its author, the first activity of its first days and a link to the full itinerary. */
export default function PublicTripCard({ summary }: Readonly<PublicTripCardProps>) {
  const { trip, firstDays } = summary;
  const days = tripDays(trip.startDate, trip.endDate).length;
  const facts = [
    trip.destination,
    days === 1 ? '1 día' : `${days} días`,
    ...(trip.budget === null ? [] : [formatBudget(trip.budget)]),
  ];

  return (
    <Card as="article" className="public-card h-100">
      <div className="public-card__image">
        <StoredImage
          id={trip.coverImageId ?? undefined}
          alt={trip.coverImageId ? `Portada de ${trip.name}` : ''}
        />
      </div>
      <div className="public-card__body">
        <Card.Title as="h3" className="h4">
          {trip.name}
        </Card.Title>
        <p className="public-card__facts">{facts.join(' · ')}</p>
        <p className="public-card__author">{`Por ${trip.authorName}`}</p>
        {firstDays.length > 0 && (
          <ul className="public-card__days" aria-label="Primeros días">
            {firstDays.map((preview) => (
              <li key={preview.day}>
                <strong>{`Día ${preview.day}`}</strong>
                {` · ${preview.activityName}`}
              </li>
            ))}
          </ul>
        )}
        <Link
          to={`/public/${trip.id}`}
          className="btn btn-primary public-card__open"
          aria-label={`Ver itinerario completo de ${trip.name}`}
        >
          Ver itinerario completo
        </Link>
      </div>
    </Card>
  );
}
