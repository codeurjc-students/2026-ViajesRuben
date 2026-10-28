import Card from 'react-bootstrap/Card';
import { Link } from 'react-router';
import type { ExampleTrip } from '../types';
import { tripDays } from '../utils/tripDays';
import { formatBudget } from '../utils/tripFormat';
import StoredImage from './StoredImage';
import './ExampleTripCard.css';

interface ExampleTripCardProps {
  example: ExampleTrip;
}

/** An example trip on the home page: cover, key facts, the first activity of its first days and a link to the full itinerary. */
export default function ExampleTripCard({ example }: Readonly<ExampleTripCardProps>) {
  const { trip, firstDays } = example;
  const days = tripDays(trip.startDate, trip.endDate).length;
  const facts = [
    trip.destination,
    days === 1 ? '1 día' : `${days} días`,
    ...(trip.budget === null ? [] : [formatBudget(trip.budget)]),
  ];

  return (
    <Card as="article" className="example-card h-100">
      <div className="example-card__image">
        <StoredImage
          id={trip.coverImageId ?? undefined}
          alt={trip.coverImageId ? `Portada de ${trip.name}` : ''}
        />
      </div>
      <div className="example-card__body">
        <Card.Title as="h3" className="h4">
          {trip.name}
        </Card.Title>
        <p className="example-card__facts">{facts.join(' · ')}</p>
        {firstDays.length > 0 && (
          <ul className="example-card__days" aria-label="Primeros días">
            {firstDays.map((preview) => (
              <li key={preview.day}>
                <strong>{`Día ${preview.day}`}</strong>
                {` · ${preview.activityName}`}
              </li>
            ))}
          </ul>
        )}
        <Link
          to={`/examples/${trip.id}`}
          className="btn btn-primary example-card__open"
          aria-label={`Ver itinerario completo de ${trip.name}`}
        >
          Ver itinerario completo
        </Link>
      </div>
    </Card>
  );
}
