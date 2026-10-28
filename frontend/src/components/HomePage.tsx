import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Row from 'react-bootstrap/Row';
import { Link } from 'react-router';
import { useAuth } from '../hooks/useAuth';
import ExampleTrips from './ExampleTrips';
import './HomePage.css';

const STEPS = [
  { title: 'Crea un viaje', text: 'Indica el destino, las fechas y el presupuesto.' },
  {
    title: 'Añade actividades día a día',
    text: 'Organiza cada día con horarios, lugares y costes.',
  },
  {
    title: 'Mantén el itinerario sin conflictos',
    text: 'TripMate avisa si dos actividades se solapan, se salen de las fechas o superan el presupuesto.',
  },
];

/** `/`: public home page. Introduces TripMate, shows the example trips and explains how it works. */
export default function HomePage() {
  const { user } = useAuth();

  return (
    <Container as="main" className="app-main py-4">
      <section className="home-hero">
        <div className="home-hero__text">
          <h1 className="home-hero__title text-white">Planifica tus viajes con TripMate</h1>
          <p className="home-hero__intro text-white">
            Crea tus viajes, organiza las actividades día a día y mantén el itinerario libre de
            conflictos. Echa un vistazo a algunos itinerarios de ejemplo.
          </p>
          <div className="d-flex flex-wrap gap-2">
            {user ? (
              <Link to="/trips" className="btn btn-light btn-lg fw-semibold">
                Ir a Mis viajes
              </Link>
            ) : (
              <>
                <Link to="/register" className="btn btn-light btn-lg fw-semibold">
                  Regístrate gratis
                </Link>
                <Link to="/login" className="btn btn-outline-light btn-lg">
                  Ya tengo cuenta
                </Link>
              </>
            )}
          </div>
        </div>
        <img src="/trip_background_image.jpg" alt="" className="home-hero__photo" />
      </section>

      <ExampleTrips />

      <section aria-labelledby="how-title" className="home-how">
        <h2 id="how-title" className="h3 mb-4">
          Cómo funciona
        </h2>
        <Row as="ol" className="home-how__steps gy-3">
          {STEPS.map((step) => (
            <Col as="li" key={step.title} md={4}>
              <h3 className="h5">{step.title}</h3>
              <p className="mb-0">{step.text}</p>
            </Col>
          ))}
        </Row>
      </section>
    </Container>
  );
}
