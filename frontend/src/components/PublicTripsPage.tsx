import { useCallback, useState, type SubmitEvent } from 'react';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Container from 'react-bootstrap/Container';
import Form from 'react-bootstrap/Form';
import Spinner from 'react-bootstrap/Spinner';
import { Link } from 'react-router';
import { usePagedList } from '../hooks/usePagedList';
import { getPublicTrips, PUBLIC_LIST_PAGE_SIZE } from '../services/publicTripService';
import { formatDateRange } from '../utils/tripFormat';
import LoadMoreButton from './LoadMoreButton';
import './PublicTripsPage.css';

interface PublicTripListProps {
  search: string;
}

/** The public trips matching `search`; mounted again (key) for every new search so it starts at page 0. */
function PublicTripList({ search }: Readonly<PublicTripListProps>) {
  const fetchPage = useCallback(
    (page: number) => getPublicTrips(page, PUBLIC_LIST_PAGE_SIZE, search),
    [search],
  );
  const { items: summaries, loaded, loading, last, error, loadMore } = usePagedList(fetchPage);

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
      {loaded && summaries.length === 0 && (
        <Card className="public-list__empty">
          <p className="mb-0">
            {search
              ? 'Ningún viaje público coincide con la búsqueda. Prueba con otro nombre o destino.'
              : 'Aún no hay viajes públicos.'}
          </p>
        </Card>
      )}
      {summaries.length > 0 && (
        <ul className="public-list__list">
          {summaries.map(({ trip }) => (
            <li key={trip.id}>
              <Card className="public-list__item">
                <h2 className="public-list__name">
                  <Link to={`/public/${trip.id}`}>{trip.name}</Link>
                </h2>
                <p className="public-list__meta">
                  {`${trip.destination} · ${formatDateRange(trip.startDate, trip.endDate)} · Por ${trip.authorName}`}
                </p>
              </Card>
            </li>
          ))}
        </ul>
      )}
      {loaded && <LoadMoreButton last={last} loading={loading} onClick={loadMore} />}
    </>
  );
}

/** Every public itinerary, newest first, with a search by trip name or destination; public, no login. */
export default function PublicTripsPage() {
  const [text, setText] = useState('');
  const [search, setSearch] = useState('');

  function handleSearch(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setSearch(text.trim());
  }

  return (
    <Container as="main" className="app-main py-4">
      <h1 className="public-list__title text-white mb-4">Viajes públicos</h1>
      <Card className="public-list__search">
        <Form role="search" onSubmit={handleSearch}>
          <Form.Label htmlFor="public-trips-search">Buscar por nombre o destino</Form.Label>
          <div className="public-list__search-row">
            <Form.Control
              id="public-trips-search"
              type="search"
              value={text}
              onChange={(event) => setText(event.target.value)}
            />
            <Button type="submit">Buscar</Button>
          </div>
        </Form>
      </Card>
      <PublicTripList key={search} search={search} />
    </Container>
  );
}
