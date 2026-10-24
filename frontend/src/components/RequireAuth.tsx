import Container from 'react-bootstrap/Container';
import Spinner from 'react-bootstrap/Spinner';
import { Navigate, Outlet, useLocation } from 'react-router';
import { useAuth } from '../hooks/useAuth';

/** Layout route: shows its child routes to logged-in users and sends everyone else to /login. */
export default function RequireAuth() {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <Container as="main" className="app-main py-4 text-center text-white">
        <Spinner animation="border" role="status">
          <span className="visually-hidden">Cargando…</span>
        </Spinner>
      </Container>
    );
  }
  if (!user) return <Navigate to="/login" replace state={{ from: location }} />;
  return <Outlet />;
}
