import { useState } from 'react';
import Card from 'react-bootstrap/Card';
import Container from 'react-bootstrap/Container';
import Button from 'react-bootstrap/Button';
import { Link, useNavigate, useParams } from 'react-router';
import { useAuth } from '../hooks/useAuth';
import { useTrip } from '../hooks/useTrip';
import { formatBudget, formatDateRange } from '../utils/tripFormat';
import DeleteTripModal from './DeleteTripModal';
import StoredImage from './StoredImage';
import './TripDetailPage.css';

/** `/trips/:tripId`: the trip's summary. Participants and the itinerary will go below it (I3, B5). */
export default function TripDetailPage() {
  const { tripId } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const { trip, error } = useTrip(tripId);
  const [confirming, setConfirming] = useState(false);

  // Rethrown so the route's error page (inside the layout) is shown.
  if (error) throw error;

  return (
    <Container as="main" className="app-main py-4">
      <p>
        <Link to="/trips" className="trip-detail__back">
          Volver a Mis viajes
        </Link>
      </p>
      {!trip ? (
        <p className="bg-white rounded p-3" role="status">
          Cargando viaje…
        </p>
      ) : (
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
            {user?.id === trip.creatorId && (
              <div className="d-flex flex-wrap gap-2">
                <Link to={`/trips/${trip.id}/edit`} className="btn btn-outline-primary">
                  Editar
                </Link>
                <Button variant="outline-danger" onClick={() => setConfirming(true)}>
                  Eliminar
                </Button>
              </div>
            )}
          </div>
          <DeleteTripModal
            trip={trip}
            show={confirming}
            onCancel={() => setConfirming(false)}
            onDeleted={() => navigate('/trips')}
          />
        </Card>
      )}
    </Container>
  );
}
