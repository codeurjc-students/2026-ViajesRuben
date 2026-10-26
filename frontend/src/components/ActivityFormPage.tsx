import { useState } from 'react';
import Card from 'react-bootstrap/Card';
import Container from 'react-bootstrap/Container';
import { useLocation, useNavigate, useParams, useSearchParams } from 'react-router';
import { useActivity } from '../hooks/useActivity';
import { useTrip } from '../hooks/useTrip';
import { applyActivityImage, createActivity, updateActivity } from '../services/activityService';
import { ApiError, NetworkError } from '../services/apiError';
import type { ActivityData, CoverChange } from '../types';
import { isDayInTrip } from '../utils/tripDays';
import ActivityForm from './ActivityForm';
import './ActivityForm.css';

function saveErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 400) return 'Revisa los datos del formulario.';
    if (error.status === 401) return 'Tu sesión ha caducado. Inicia sesión de nuevo.';
    if (error.status === 403) return 'No puedes modificar este viaje.';
    if (error.status === 404) return 'Esta actividad ya no existe.';
  }
  if (error instanceof NetworkError) {
    return 'No se pudo conectar con el servidor. Inténtalo de nuevo.';
  }
  return 'Algo ha fallado en el servidor. Inténtalo de nuevo más tarde.';
}

/** `/trips/:tripId/activities/new` and `/trips/:tripId/activities/:activityId/edit`: one form for both. */
export default function ActivityFormPage() {
  const { tripId, activityId } = useParams();
  const editing = activityId !== undefined;
  const navigate = useNavigate();
  const location = useLocation();
  const [params] = useSearchParams();
  const notice = (location.state as { notice?: string } | null)?.notice;
  const { trip, error: tripError } = useTrip(tripId);
  const { activity, error: activityError } = useActivity(tripId, activityId);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string>();

  // Rethrown so the route's error page (inside the layout) is shown.
  if (tripError) throw tripError;
  if (editing && activityError) throw activityError;

  /** Where the user comes from: the trip page on the given day. */
  function tripPage(day?: string) {
    return day ? `/trips/${tripId}?day=${day}` : `/trips/${tripId}`;
  }

  async function save(data: ActivityData, image: CoverChange) {
    if (!trip) return;
    setSubmitting(true);
    setSubmitError(undefined);
    let saved;
    try {
      saved = activity
        ? await updateActivity(trip.id, activity.id, data)
        : await createActivity(trip.id, data);
    } catch (error) {
      setSubmitError(saveErrorMessage(error));
      setSubmitting(false);
      return;
    }
    try {
      await applyActivityImage(trip.id, saved.id, image);
    } catch {
      // The data is saved: go to the edit page, where the image can be tried again.
      setSubmitting(false);
      navigate(`/trips/${trip.id}/activities/${saved.id}/edit`, {
        replace: true,
        state: {
          notice:
            'La actividad se ha guardado, pero no se pudo actualizar la imagen. Elige la imagen de nuevo.',
        },
      });
      return;
    }
    navigate(tripPage(saved.date));
  }

  const loading = !trip || (editing && !activity);
  const requestedDay = params.get('day');
  const initialDate =
    trip && requestedDay && isDayInTrip(requestedDay, trip.startDate, trip.endDate)
      ? requestedDay
      : trip?.startDate;

  return (
    <Container as="main" className="app-main py-4">
      <div className="activity-form-page">
        <h1 className="activity-form-page__title mb-2 text-white">
          {editing ? 'Editar actividad' : 'Nueva actividad'}
        </h1>
        {trip && <p className="text-white mb-4">{`Viaje: ${trip.name}`}</p>}
        {loading ? (
          <p className="bg-white rounded p-3" role="status">
            Cargando actividad…
          </p>
        ) : (
          <Card className="activity-form-card">
            <ActivityForm
              trip={trip}
              activity={activity}
              initialDate={initialDate}
              submitting={submitting}
              submitError={submitError}
              notice={notice}
              onSubmit={save}
              onCancel={() => navigate(tripPage(activity?.date ?? initialDate))}
            />
          </Card>
        )}
      </div>
    </Container>
  );
}
