import { useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Row from 'react-bootstrap/Row';
import { Link, useLocation } from 'react-router';
import { usePagedList } from '../hooks/usePagedList';
import { getTrips } from '../services/tripService';
import EmptyTrips from './EmptyTrips';
import LoadMoreButton from './LoadMoreButton';
import PendingInvitations from './PendingInvitations';
import TripCard from './TripCard';
import './TripList.css';

export default function TripList() {
  const {
    items: trips,
    totalElements,
    loaded,
    loading,
    last,
    error,
    loadMore,
    reload,
  } = usePagedList(getTrips);
  // A message left by the page that sent the user here, e.g. after leaving a trip (I3).
  const notice = (useLocation().state as { notice?: string } | null)?.notice;
  const [noticeDismissed, setNoticeDismissed] = useState(false);

  // Rethrown so the route's error page (inside the layout) is shown.
  if (error) throw error;

  return (
    <Container as="main" className="app-main py-4">
      <div className="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
        <div className="d-flex flex-wrap align-items-center gap-3">
          <h1 className="trip-list__title mb-0 text-white">Mis viajes</h1>
          {loaded && trips.length > 0 && (
            <span className="trip-list__count">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M8 7V5a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v2M4 7h16v12H4ZM4 12h16" />
              </svg>
              {totalElements === 1 ? '1 viaje' : `${totalElements} viajes`}
            </span>
          )}
        </div>
        <Link to="/trips/new" className="btn btn-light btn-lg trip-list__create fw-semibold">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 5v14M5 12h14" />
          </svg>
          Nuevo viaje
        </Link>
      </div>
      {notice && !noticeDismissed && (
        <Alert variant="success" dismissible onClose={() => setNoticeDismissed(true)}>
          {notice}
        </Alert>
      )}
      <PendingInvitations onAccepted={reload} />
      {loaded && trips.length === 0 && <EmptyTrips />}
      {trips.length > 0 && (
        <Row as="ul" id="trips" className="trip-list gy-4">
          {trips.map((trip) => (
            <Col as="li" key={trip.id} xs={12} lg={6}>
              <TripCard trip={trip} onDeleted={reload} />
            </Col>
          ))}
        </Row>
      )}
      {loaded && <LoadMoreButton last={last} loading={loading} onClick={loadMore} />}
    </Container>
  );
}
