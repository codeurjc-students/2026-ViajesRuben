import { useEffect, useState } from 'react';
import Button from 'react-bootstrap/Button';
import Col from 'react-bootstrap/Col';
import Row from 'react-bootstrap/Row';
import { Link } from 'react-router';
import {
  CAROUSEL_MAX_PAGES,
  getPublicTrips,
  PUBLIC_PAGE_SIZE,
} from '../services/publicTripService';
import type { Page, PublicTripSummary } from '../types';
import PublicTripCard from './PublicTripCard';
import './PublicTripsCarousel.css';

/** The answer for one page, or the reason it failed. */
interface Loaded {
  page: number;
  data?: Page<PublicTripSummary>;
  error?: unknown;
}

/** The most recently published trips, three at a time and at most nine, with previous and next buttons. */
export default function PublicTripsCarousel() {
  const [page, setPage] = useState(0);
  const [loaded, setLoaded] = useState<Loaded>();

  useEffect(() => {
    let cancelled = false;
    getPublicTrips(page)
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
  const shown = Math.min(loaded?.data?.totalElements ?? 0, PUBLIC_PAGE_SIZE * CAROUSEL_MAX_PAGES);
  const current = loaded?.page === page ? loaded.data : undefined;
  const hasPages = shown > PUBLIC_PAGE_SIZE;
  const atLastPage = !current || current.last || page >= CAROUSEL_MAX_PAGES - 1;

  return (
    <section aria-labelledby="public-title" className="public-trips">
      <div className="public-trips__header">
        <div>
          <div className="public-trips__title-row">
            <h2 id="public-title" className="h1 text-white mb-0">
              Itinerarios públicos
            </h2>
            <Link to="/public" className="public-trips__all">
              Ver todos
            </Link>
          </div>
          <p className="text-white mb-0">
            Viajes publicados por otros viajeros. Ábrelos para ver el itinerario día a día.
          </p>
        </div>
        {hasPages && (
          <div className="public-trips__pager">
            <Button
              variant="light"
              aria-label="Itinerarios anteriores"
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
            >
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M15 5l-7 7 7 7" />
              </svg>
            </Button>
            <Button
              variant="light"
              aria-label="Itinerarios siguientes"
              disabled={atLastPage}
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
        <output className="d-block mb-3 bg-white rounded p-3">
          Cargando itinerarios públicos…
        </output>
      )}
      {current && current.content.length === 0 && (
        <p className="bg-white rounded p-3">Todavía no hay itinerarios públicos.</p>
      )}
      {current && current.content.length > 0 && (
        <Row as="ul" className="public-trips__list gy-4">
          {current.content.map((summary) => (
            <Col as="li" key={summary.trip.id} xs={12} lg={4}>
              <PublicTripCard summary={summary} />
            </Col>
          ))}
        </Row>
      )}
    </section>
  );
}
