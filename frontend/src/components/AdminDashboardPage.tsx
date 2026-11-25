import Card from 'react-bootstrap/Card';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Row from 'react-bootstrap/Row';
import Spinner from 'react-bootstrap/Spinner';
import { Link } from 'react-router';
import { usePlatformMetrics } from '../hooks/usePlatformMetrics';
import type { PlatformMetrics } from '../types';
import './AdminDashboardPage.css';

/** One card per platform total; a new total is one more entry here and one more field in the type. */
const CARDS: { key: keyof PlatformMetrics; label: string }[] = [
  { key: 'users', label: 'Usuarios' },
  { key: 'trips', label: 'Viajes' },
  { key: 'activities', label: 'Actividades' },
];

/** The administrator's dashboard: the platform totals and the way to user and trip management. */
export default function AdminDashboardPage() {
  const { metrics, error } = usePlatformMetrics();

  // Rethrown so the route's error page (inside the layout) is shown.
  if (error) throw error;

  return (
    <Container as="main" className="app-main py-4">
      <h1 className="admin-dashboard__title text-white mb-4">Panel de administración</h1>
      {metrics ? (
        <Row as="ul" xs={1} md={3} className="admin-dashboard__cards g-3 list-unstyled">
          {CARDS.map(({ key, label }) => (
            <Col as="li" key={key}>
              <Card className="admin-dashboard__card h-100">
                <h2 className="admin-dashboard__label">{label}</h2>
                <p className="admin-dashboard__value">{metrics[key].toLocaleString('es-ES')}</p>
              </Card>
            </Col>
          ))}
        </Row>
      ) : (
        <div className="text-center text-white py-4">
          <Spinner animation="border" role="status">
            <span className="visually-hidden">Cargando métricas…</span>
          </Spinner>
        </div>
      )}
      <div className="admin-dashboard__links">
        <Link to="/admin/users" className="btn btn-outline-primary">
          Gestionar usuarios
        </Link>
        <Link to="/admin/trips" className="btn btn-outline-primary">
          Moderar viajes
        </Link>
      </div>
    </Container>
  );
}
