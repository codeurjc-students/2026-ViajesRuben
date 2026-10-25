import Card from 'react-bootstrap/Card';
import Col from 'react-bootstrap/Col';
import Row from 'react-bootstrap/Row';
import { Link } from 'react-router';
import StoredImage from './StoredImage';
import './EmptyTrips.css';
import './TripCard.css';

/** "My trips" without trips: a sketch of a trip card, to be filled in with the first one. */
export default function EmptyTrips() {
  return (
    <Row>
      <Col xs={12} lg={6}>
        <Card as="section" className="trip-card trip-empty" aria-labelledby="trip-empty-title">
          <div className="trip-card__image">
            <StoredImage />
          </div>
          <div className="trip-card__info">
            <h2 id="trip-empty-title" className="h4">
              Tu primer viaje te está esperando
            </h2>
            <p className="trip-card__meta">Destino: ¿A dónde te apetece ir?</p>
            <p className="trip-card__meta">Fechas: elige cuándo viajas</p>
            <p>Ponle nombre, elige una portada y empieza a planificarlo.</p>
            <Link to="/trips/new" className="btn btn-primary">
              Añadir tu primer viaje
            </Link>
          </div>
        </Card>
      </Col>
    </Row>
  );
}
