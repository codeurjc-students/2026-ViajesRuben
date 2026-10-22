import Button from 'react-bootstrap/Button';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Row from 'react-bootstrap/Row';
import { usePagedList } from '../hooks/usePagedList';
import { getTrips } from '../services/tripService';
import LoadMoreButton from './LoadMoreButton';
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
  } = usePagedList(getTrips);

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
        <Button variant="light" size="lg" className="trip-list__create fw-semibold">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 5v14M5 12h14" />
          </svg>
          Crear nuevo viaje
        </Button>
      </div>
      {loaded && trips.length === 0 && (
        <p className="bg-white rounded p-3">Aún no tienes viajes. Crea el primero.</p>
      )}
      {trips.length > 0 && (
        <Row as="ul" id="trips" className="trip-list gy-4">
          {trips.map((trip) => (
            <Col as="li" key={trip.id} xs={12} md={6}>
              <TripCard trip={trip} />
            </Col>
          ))}
        </Row>
      )}
      {loaded && <LoadMoreButton last={last} loading={loading} onClick={loadMore} />}
    </Container>
  );
}
