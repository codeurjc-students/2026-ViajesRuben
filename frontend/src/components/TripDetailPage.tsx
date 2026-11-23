import { useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Badge from 'react-bootstrap/Badge';
import Card from 'react-bootstrap/Card';
import Container from 'react-bootstrap/Container';
import Button from 'react-bootstrap/Button';
import { Link, useLocation, useNavigate, useParams } from 'react-router';
import { useAuth } from '../hooks/useAuth';
import { useConflicts } from '../hooks/useConflicts';
import { useTrip } from '../hooks/useTrip';
import { useTripCost } from '../hooks/useTripCost';
import { setPublication } from '../services/tripService';
import type { Trip } from '../types';
import { formatBudget, formatDateRange } from '../utils/tripFormat';
import ActivityLogSection from './ActivityLogSection';
import ConflictAlert from './ConflictAlert';
import DeleteTripModal from './DeleteTripModal';
import InvitationsSection from './InvitationsSection';
import ItinerarySection from './ItinerarySection';
import ParticipantsSection from './ParticipantsSection';
import StoredImage from './StoredImage';
import './TripDetailPage.css';

/**
 * `/trips/:tripId`: the trip's summary and its itinerary. The creator also sees the invitations
 * (I1) and the participants (I3). A participant (I2) has no button to change the trip itself, but
 * plans its itinerary together with the creator (I4), and sees who else joined. Everyone who opens
 * the trip, the administrator included, reads its change log (I4) below the itinerary. The
 * administrator reviewing another user's trip (B8) sees it read-only, with only a delete button.
 */
export default function TripDetailPage() {
  const { tripId } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  // A message left by the page that sent the user here, e.g. after copying a public itinerary (I9).
  const notice = (useLocation().state as { notice?: string } | null)?.notice;
  const [noticeDismissed, setNoticeDismissed] = useState(false);
  const { trip, error } = useTrip(tripId);
  const { conflicts, reload: reloadConflicts } = useConflicts(trip?.id);
  const { cost, reload: reloadCost } = useTripCost(trip?.id);
  const [confirming, setConfirming] = useState(false);
  // The trip as the last publication change left it; the loaded one until the creator changes it.
  const [updated, setUpdated] = useState<Trip>();
  const [saving, setSaving] = useState(false);
  const [publicationFailed, setPublicationFailed] = useState(false);
  // Changes with every deletion made here, so the change log is read again (it remounts).
  const [logVersion, setLogVersion] = useState(0);
  const isCreator = trip !== undefined && user?.id === trip.creatorId;
  const moderating = trip !== undefined && !isCreator && user?.role === 'ADMIN';
  // The server only lets the creator, the administrator and the participants read a trip.
  const isParticipant = trip !== undefined && !isCreator && !moderating;
  // The creator and the participants plan the itinerary together; the administrator only reads.
  const canEditItinerary = isCreator || isParticipant;
  const published = (updated?.id === trip?.id ? updated : trip)?.published ?? false;

  // Rethrown so the route's error page (inside the layout) is shown.
  if (error) throw error;

  async function togglePublication(id: number) {
    setSaving(true);
    setPublicationFailed(false);
    try {
      setUpdated(await setPublication(id, !published));
    } catch {
      setPublicationFailed(true);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Container as="main" className="app-main py-4">
      <p>
        <Link to={moderating ? '/admin/trips' : '/trips'} className="trip-detail__back">
          {moderating ? 'Volver a Viajes' : 'Volver a Mis viajes'}
        </Link>
      </p>
      {notice && !noticeDismissed && (
        <Alert variant="success" dismissible onClose={() => setNoticeDismissed(true)}>
          {notice}
        </Alert>
      )}
      {!trip ? (
        <output className="d-block mb-3 bg-white rounded p-3">Cargando viaje…</output>
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
            {published && (
              <p>
                <Badge bg="success">Público</Badge>
              </p>
            )}
            <dl className="trip-detail__facts">
              <dt>Destino</dt>
              <dd>{trip.destination}</dd>
              <dt>Fechas</dt>
              <dd>{formatDateRange(trip.startDate, trip.endDate)}</dd>
              {trip.budget !== null && (
                <>
                  <dt>Presupuesto</dt>
                  <dd>{formatBudget(trip.budget)}</dd>
                  {cost?.remainingBudget != null && (
                    <>
                      <dt>Presupuesto restante</dt>
                      <dd className={cost.remainingBudget < 0 ? 'text-danger' : undefined}>
                        {formatBudget(cost.remainingBudget)}
                      </dd>
                    </>
                  )}
                </>
              )}
            </dl>
            {trip.description && <p className="trip-detail__description">{trip.description}</p>}
            {moderating && (
              <div className="d-flex flex-wrap gap-2">
                <Button variant="outline-danger" onClick={() => setConfirming(true)}>
                  Eliminar viaje
                </Button>
              </div>
            )}
            {isCreator && publicationFailed && (
              <Alert variant="danger" role="alert">
                No se pudo cambiar la visibilidad del viaje. Inténtalo de nuevo.
              </Alert>
            )}
            {isCreator && (
              <div className="d-flex flex-wrap gap-2">
                <Button
                  variant={published ? 'outline-secondary' : 'outline-success'}
                  disabled={saving}
                  onClick={() => togglePublication(trip.id)}
                >
                  {published ? 'Despublicar' : 'Publicar'}
                </Button>
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
            onDeleted={() => navigate(moderating ? '/admin/trips' : '/trips')}
          />
        </Card>
      )}
      {trip && <ConflictAlert tripId={trip.id} conflicts={conflicts} canEdit={canEditItinerary} />}
      {trip && (
        <ItinerarySection
          trip={trip}
          canEdit={canEditItinerary}
          onActivityDeleted={() => {
            reloadConflicts();
            reloadCost();
            setLogVersion((version) => version + 1);
          }}
        />
      )}
      {trip && <ActivityLogSection key={logVersion} tripId={trip.id} />}
      {trip && (isCreator || isParticipant) && (
        <ParticipantsSection tripId={trip.id} tripName={trip.name} canRemove={isCreator} />
      )}
      {trip && isCreator && <InvitationsSection tripId={trip.id} />}
    </Container>
  );
}
