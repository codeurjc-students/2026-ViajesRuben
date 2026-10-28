import { useEffect, useState } from 'react';
import Button from 'react-bootstrap/Button';
import Col from 'react-bootstrap/Col';
import Row from 'react-bootstrap/Row';
import { EXAMPLES_PAGE_SIZE, getExampleTrips } from '../services/exampleService';
import type { ExampleTrip, Page } from '../types';
import ExampleTripCard from './ExampleTripCard';
import './ExampleTrips.css';

/** The answer for one page, or the reason it failed. */
interface Loaded {
  page: number;
  data?: Page<ExampleTrip>;
  error?: unknown;
}

/** The example trips of the home page, three at a time, with previous and next buttons. */
export default function ExampleTrips() {
  const [page, setPage] = useState(0);
  const [loaded, setLoaded] = useState<Loaded>();

  useEffect(() => {
    let cancelled = false;
    getExampleTrips(page)
      .then((data) => {
        if (!cancelled) setLoaded({ page, data });
      })
      .catch((error) => {
        if (!cancelled) setLoaded({ page, error });
      });
    return () => {
      cancelled = true;
    };
  }, [page]);

  // Rethrown so the route's error page (inside the layout) is shown.
  if (loaded?.error) throw loaded.error;

  // The last answer decides whether there are pages at all, so the buttons stay while the next one loads.
  const total = loaded?.data?.totalElements ?? 0;
  const current = loaded?.page === page ? loaded.data : undefined;
  const hasPages = total > EXAMPLES_PAGE_SIZE;

  return (
    <section aria-labelledby="examples-title" className="example-trips">
      <div className="example-trips__header">
        <div>
          <h2 id="examples-title" className="h1 text-white">
            Itinerarios de ejemplo
          </h2>
          <p className="text-white mb-0">
            Viajes de muestra hechos con TripMate. Ábrelos para ver el itinerario día a día.
          </p>
        </div>
        {hasPages && (
          <div className="example-trips__pager">
            <Button
              variant="light"
              aria-label="Ejemplos anteriores"
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
            >
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M15 5l-7 7 7 7" />
              </svg>
            </Button>
            <Button
              variant="light"
              aria-label="Ejemplos siguientes"
              disabled={!current || current.last}
              onClick={() => setPage(page + 1)}
            >
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M9 5l7 7-7 7" />
              </svg>
            </Button>
          </div>
        )}
      </div>
      {!current && (
        <p className="bg-white rounded p-3" role="status">
          Cargando itinerarios de ejemplo…
        </p>
      )}
      {current && current.content.length === 0 && (
        <p className="bg-white rounded p-3">Todavía no hay itinerarios de ejemplo.</p>
      )}
      {current && current.content.length > 0 && (
        <Row as="ul" className="example-trips__list gy-4">
          {current.content.map((example) => (
            <Col as="li" key={example.trip.id} xs={12} lg={4}>
              <ExampleTripCard example={example} />
            </Col>
          ))}
        </Row>
      )}
    </section>
  );
}
