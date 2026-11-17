import { useCallback, useState, type SubmitEvent } from 'react';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Container from 'react-bootstrap/Container';
import Form from 'react-bootstrap/Form';
import Spinner from 'react-bootstrap/Spinner';
import Alert from 'react-bootstrap/Alert';
import { Link, useLocation } from 'react-router';
import { useAuth } from '../hooks/useAuth';
import { usePagedList } from '../hooks/usePagedList';
import { getPublicTrips, PUBLIC_LIST_PAGE_SIZE } from '../services/publicTripService';
import { formatDateRange } from '../utils/tripFormat';
import LoadMoreButton from './LoadMoreButton';
import UnpublishTripModal from './UnpublishTripModal';
import './PublicTripsPage.css';

interface PublicTripListProps {
  search: string;
  /** Set for the administrator: called with the trip to unpublish. */
  onUnpublish?: (trip: { id: number; name: string }) => void;
}

/** The public trips matching `search`; mounted again (key) for every new search so it starts at page 0. */
function PublicTripList({ search, onUnpublish }: Readonly<PublicTripListProps>) {
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
                {onUnpublish && (
                  <Button
                    variant="outline-danger"
                    size="sm"
                    className="public-list__unpublish"
                    aria-label={`Despublicar ${trip.name}`}
                    onClick={() => onUnpublish(trip)}
                  >
                    Despublicar
                  </Button>
                )}
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
  const { user } = useAuth();
  const location = useLocation();
  const [text, setText] = useState('');
  const [search, setSearch] = useState('');
  const [unpublishing, setUnpublishing] = useState<{ id: number; name: string }>();
  const [reloads, setReloads] = useState(0);
  const [notice, setNotice] = useState((location.state as { notice?: string } | null)?.notice);
  const isAdmin = user?.role === 'ADMIN';

  function unpublished() {
    setNotice(`«${unpublishing?.name}» ya no es público.`);
    setUnpublishing(undefined);
    setReloads((count) => count + 1);
  }

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
      {notice && (
        <Alert variant="success" dismissible onClose={() => setNotice(undefined)}>
          {notice}
        </Alert>
      )}
      <PublicTripList
        key={`${search}-${reloads}`}
        search={search}
        onUnpublish={isAdmin ? setUnpublishing : undefined}
      />
      {unpublishing && (
        <UnpublishTripModal
          trip={unpublishing}
          show
          onCancel={() => setUnpublishing(undefined)}
          onUnpublished={unpublished}
        />
      )}
    </Container>
  );
}
