import { useState } from 'react';
import Card from 'react-bootstrap/Card';
import Container from 'react-bootstrap/Container';
import { useLocation, useNavigate, useParams } from 'react-router';
import { useTrip } from '../hooks/useTrip';
import { ApiError, NetworkError } from '../services/apiError';
import { applyCover, createTrip, updateTrip } from '../services/tripService';
import type { CoverChange, TripData } from '../types';
import TripForm from './TripForm';

function saveErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 400) return 'Revisa los datos del formulario.';
    if (error.status === 401) return 'Tu sesión ha caducado. Inicia sesión de nuevo.';
    if (error.status === 403) return 'No puedes modificar este viaje.';
    if (error.status === 404) return 'Este viaje ya no existe.';
  }
  if (error instanceof NetworkError) {
    return 'No se pudo conectar con el servidor. Inténtalo de nuevo.';
  }
  return 'Algo ha fallado en el servidor. Inténtalo de nuevo más tarde.';
}

/** `/trips/new` and `/trips/:tripId/edit`: the same form creates a trip or edits one. */
export default function TripFormPage() {
  const { tripId } = useParams();
  const editing = tripId !== undefined;
  const navigate = useNavigate();
  const location = useLocation();
  const notice = (location.state as { notice?: string } | null)?.notice;
  const { trip, error: loadError } = useTrip(tripId);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string>();

  // Rethrown so the route's error page (inside the layout) is shown.
  if (editing && loadError) throw loadError;

  async function save(data: TripData, cover: CoverChange) {
    setSubmitting(true);
    setSubmitError(undefined);
    let saved;
    try {
      saved = trip ? await updateTrip(trip.id, data) : await createTrip(data);
    } catch (error) {
      setSubmitError(saveErrorMessage(error));
      setSubmitting(false);
      return;
    }
    try {
      await applyCover(saved.id, cover);
    } catch {
      // The data is saved: go to the edit page, where the cover can be tried again.
      setSubmitting(false);
      navigate(`/trips/${saved.id}/edit`, {
        replace: true,
        state: {
          notice:
            'El viaje se ha guardado, pero no se pudo actualizar la portada. Elige la imagen de nuevo.',
        },
      });
      return;
    }
    navigate(`/trips/${saved.id}`);
  }

  const loading = editing && !trip;

  return (
    <Container as="main" className="app-main py-4">
      <div className="trip-form-page">
        <h1 className="trip-form-page__title mb-4 text-white">
          {editing ? 'Editar viaje' : 'Nuevo viaje'}
        </h1>
        {loading ? (
          <p className="bg-white rounded p-3" role="status">
            Cargando viaje…
          </p>
        ) : (
          <Card className="trip-form-card">
            <TripForm
              trip={trip}
              submitting={submitting}
              submitError={submitError}
              notice={notice}
              onSubmit={save}
              onCancel={() => navigate(trip ? `/trips/${trip.id}` : '/trips')}
            />
          </Card>
        )}
      </div>
    </Container>
  );
}
