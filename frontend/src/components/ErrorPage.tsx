import Container from 'react-bootstrap/Container';
import { Link, isRouteErrorResponse, useRouteError } from 'react-router';
import { ApiError, NetworkError } from '../services/apiError';
import './StatusPanel.css';

interface ErrorMessage {
  title: string;
  detail: string;
}

const NOT_FOUND: ErrorMessage = {
  title: 'No encontramos esta página',
  detail:
    'Puede que la dirección esté mal escrita o que la página ya no exista. Vuelve al inicio para seguir.',
};

const FORBIDDEN: ErrorMessage = {
  title: 'No tienes acceso a esta página',
  detail: 'Este contenido pertenece a otro usuario. Vuelve al inicio para seguir.',
};

const SESSION_EXPIRED: ErrorMessage = {
  title: 'Tu sesión ha caducado',
  detail: 'Inicia sesión de nuevo para continuar.',
};

const NO_CONNECTION: ErrorMessage = {
  title: 'No hay conexión con el servidor',
  detail: 'Comprueba tu conexión a internet e inténtalo de nuevo en unos minutos.',
};

const SERVER_ERROR: ErrorMessage = {
  title: 'Algo ha fallado en el servidor',
  detail: 'No es culpa tuya. Inténtalo de nuevo en unos minutos.',
};

function messageFor(error: unknown): ErrorMessage {
  if (isRouteErrorResponse(error) && error.status === 404) return NOT_FOUND;
  if (error instanceof ApiError && error.status === 404) return NOT_FOUND;
  if (error instanceof ApiError && error.status === 403) return FORBIDDEN;
  if (error instanceof ApiError && error.status === 401) return SESSION_EXPIRED;
  if (error instanceof NetworkError) return NO_CONNECTION;
  return SERVER_ERROR;
}

interface ErrorPageProps {
  /** Set by the catch-all route, where no error object exists. */
  notFound?: boolean;
}

/** Shown inside the app layout for unknown routes and for failures while loading data. */
export default function ErrorPage({ notFound = false }: Readonly<ErrorPageProps>) {
  const routeError = useRouteError();
  const { title, detail } = notFound ? NOT_FOUND : messageFor(routeError);

  return (
    <Container as="main" className="app-main py-4">
      <div className="status-panel">
        <div className="error-page__line" aria-hidden="true">
          <span />
          <span />
        </div>
        <h1 className="h2">{title}</h1>
        <p>{detail}</p>
        <Link to="/" className="btn btn-primary">
          Volver al inicio
        </Link>
      </div>
    </Container>
  );
}
