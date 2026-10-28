import Card from 'react-bootstrap/Card';
import Container from 'react-bootstrap/Container';
import { Link, useParams } from 'react-router';
import { useTrip } from '../hooks/useTrip';
import { getExampleTrip } from '../services/exampleService';
import { formatBudget, formatDateRange } from '../utils/tripFormat';
import ItinerarySection from './ItinerarySection';
import StoredImage from './StoredImage';
import './TripDetailPage.css';

/** `/examples/:tripId`: read-only preview of an example trip, public. No edit, delete or conflicts. */
export default function ExampleTripPage() {
  const { tripId } = useParams();
  const { trip, error } = useTrip(tripId, getExampleTrip);

  // Rethrown so the route's error page (inside the layout) is shown.
  if (error) throw error;

  return (
    <Container as="main" className="app-main py-4">
      <p>
        <Link to="/" className="trip-detail__back">
          Volver al inicio
        </Link>
      </p>
      {!trip ? (
        <p className="bg-white rounded p-3" role="status">
          Cargando viaje…
        </p>
      ) : (
        <>
          <Card as="article" className="trip-detail">
            <div className="trip-detail__cover">
              <StoredImage
                id={trip.coverImageId ?? undefined}
                alt={trip.coverImageId ? `Portada de ${trip.name}` : ''}
              />
            </div>
            <div className="trip-detail__body">
              <h1 className="trip-detail__title">{trip.name}</h1>
              <dl className="trip-detail__facts">
                <dt>Destino</dt>
                <dd>{trip.destination}</dd>
                <dt>Fechas</dt>
                <dd>{formatDateRange(trip.startDate, trip.endDate)}</dd>
                {trip.budget !== null && (
                  <>
                    <dt>Presupuesto</dt>
                    <dd>{formatBudget(trip.budget)}</dd>
                  </>
                )}
              </dl>
              {trip.description && <p className="trip-detail__description">{trip.description}</p>}
            </div>
          </Card>
          <ItinerarySection trip={trip} example />
        </>
      )}
    </Container>
  );
}
