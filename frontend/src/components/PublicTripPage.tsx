import { useState } from 'react';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Container from 'react-bootstrap/Container';
import { Link, useLocation, useNavigate, useParams } from 'react-router';
import { useAuth } from '../hooks/useAuth';
import { useTrip } from '../hooks/useTrip';
import { getPublicTrip } from '../services/publicTripService';
import type { Trip } from '../types';
import { formatBudget, formatDateRange } from '../utils/tripFormat';
import CopyTripModal from './CopyTripModal';
import ItinerarySection from './ItinerarySection';
import StoredImage from './StoredImage';
import UnpublishTripModal from './UnpublishTripModal';
import './TripDetailPage.css';

/**
 * `/public/:tripId`: read-only preview of a public itinerary, no login needed. No edit, delete or
 * conflicts. Copying it to one's own trips (I9) needs a login: a visitor is sent to the login page
 * and brought back here.
 */
export default function PublicTripPage() {
  const { tripId } = useParams();
  const { user } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const { trip, error } = useTrip(tripId, getPublicTrip);
  const [copying, setCopying] = useState(false);
  const [unpublishing, setUnpublishing] = useState(false);
  const isAdmin = user?.role === 'ADMIN';

  // Rethrown so the route's error page (inside the layout) is shown.
  if (error) throw error;

  function startCopy() {
    if (user) setCopying(true);
    else navigate('/login', { state: { from: location } });
  }

  function copied(copy: Trip) {
    navigate(`/trips/${copy.id}`, {
      state: { notice: 'Itinerario copiado en tus viajes. Ya puedes editarlo.' },
    });
  }

  function unpublished(name: string) {
    navigate('/public', { state: { notice: `«${name}» ya no es público.` } });
  }

  return (
    <Container as="main" className="app-main py-4">
      <p>
        <Link to="/" className="trip-detail__back">
          Volver al inicio
        </Link>
      </p>
      {!trip ? (
        <output className="d-block mb-3 bg-white rounded p-3">Cargando viaje…</output>
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
                <dt>Autor</dt>
                <dd>{trip.authorName}</dd>
              </dl>
              {trip.description && <p className="trip-detail__description">{trip.description}</p>}
              <div className="d-flex flex-wrap gap-2">
                <Button onClick={startCopy}>Copiar en tu cuenta</Button>
                {isAdmin && (
                  <Button variant="outline-danger" onClick={() => setUnpublishing(true)}>
                    Despublicar
                  </Button>
                )}
              </div>
            </div>
          </Card>
          {user && (
            <CopyTripModal
              tripId={trip.id}
              show={copying}
              onCancel={() => setCopying(false)}
              onCopied={copied}
            />
          )}
          {isAdmin && (
            <UnpublishTripModal
              trip={trip}
              show={unpublishing}
              onCancel={() => setUnpublishing(false)}
              onUnpublished={() => unpublished(trip.name)}
            />
          )}
          <ItinerarySection trip={trip} publicPreview />
        </>
      )}
    </Container>
  );
}
