import { useCallback, useState, type SubmitEvent } from 'react';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Container from 'react-bootstrap/Container';
import Form from 'react-bootstrap/Form';
import Spinner from 'react-bootstrap/Spinner';
import { Link } from 'react-router';
import { usePagedList } from '../hooks/usePagedList';
import { getTrips } from '../services/adminTripService';
import { formatDateRange } from '../utils/tripFormat';
import LoadMoreButton from './LoadMoreButton';
import './AdminTripsPage.css';

interface AdminTripListProps {
  search: string;
}

/** The trips matching `search`; mounted again (key) for every new search so it starts at page 0. */
function AdminTripList({ search }: Readonly<AdminTripListProps>) {
  const fetchPage = useCallback((page: number) => getTrips(search, page), [search]);
  const { items: trips, loaded, loading, last, error, loadMore } = usePagedList(fetchPage);

  // Rethrown so the route's error page (inside the layout) is shown.
  if (error) throw error;

  return (
    <>
      {!loaded && (
        <div className="text-center text-white py-4">
          <Spinner animation="border" role="status">
            <span className="visually-hidden">Cargando…</span>
          </Spinner>
        </div>
      )}
      {loaded && trips.length === 0 && (
        <Card className="admin-trips__empty">
          <p className="mb-0">
            {search ? 'Ningún viaje coincide con la búsqueda.' : 'Aún no hay viajes.'}
          </p>
        </Card>
      )}
      {trips.length > 0 && (
        <ul className="admin-trips__list">
          {trips.map((trip) => (
            <li key={trip.id}>
              <Card className="admin-trip">
                <div className="admin-trip__info">
                  <h2 className="admin-trip__name">{trip.name}</h2>
                  <p className="admin-trip__meta">{trip.destination}</p>
                  <p className="admin-trip__meta">
                    {formatDateRange(trip.startDate, trip.endDate)}
                  </p>
                  <p className="admin-trip__meta">{`Creado por ${trip.creatorEmail}`}</p>
                </div>
                <div className="admin-trip__actions">
                  <Link
                    to={`/trips/${trip.id}`}
                    className="btn btn-outline-primary btn-sm"
                    aria-label={`Ver viaje ${trip.name}`}
                  >
                    Ver viaje
                  </Link>
                </div>
              </Card>
            </li>
          ))}
        </ul>
      )}
      {loaded && <LoadMoreButton last={last} loading={loading} onClick={loadMore} />}
    </>
  );
}

/** Trip moderation for the administrator: search every trip and open one to review or delete it. */
export default function AdminTripsPage() {
  const [text, setText] = useState('');
  const [search, setSearch] = useState('');

  function handleSearch(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setSearch(text.trim());
  }

  return (
    <Container as="main" className="app-main py-4">
      <h1 className="admin-trips__title text-white mb-4">Viajes</h1>
      <Card className="admin-trips__search">
        <Form role="search" onSubmit={handleSearch}>
          <Form.Label htmlFor="admin-trips-search">
            Buscar por nombre, destino o email del creador
          </Form.Label>
          <div className="admin-trips__search-row">
            <Form.Control
              id="admin-trips-search"
              type="search"
              value={text}
              onChange={(event) => setText(event.target.value)}
            />
            <Button type="submit">Buscar</Button>
          </div>
        </Form>
      </Card>
      <AdminTripList key={search} search={search} />
    </Container>
  );
}
