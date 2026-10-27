import Alert from 'react-bootstrap/Alert';
import { Link } from 'react-router';
import type { TripConflicts } from '../types';
import { formatBudget, formatDayLabel, formatTime, formatTripDate } from '../utils/tripFormat';

interface ConflictAlertProps {
  tripId: number;
  conflicts?: TripConflicts;
  /** Only the creator can edit an activity, so only the creator gets the "Editar" links. */
  canEdit: boolean;
}

/** The warnings about the itinerary, above it. Nothing is shown when there are none. */
export default function ConflictAlert({
  tripId,
  conflicts,
  canEdit,
}: Readonly<ConflictAlertProps>) {
  if (!conflicts) return null;
  const { overlaps, outsideDates, budget } = conflicts;
  if (overlaps.length === 0 && outsideDates.length === 0 && !budget) return null;

  return (
    <Alert variant="warning" className="mt-4 mb-0">
      <Alert.Heading as="h2" className="h5">
        Conflictos en el itinerario
      </Alert.Heading>
      <ul className="mb-0">
        {overlaps.map((overlap) => (
          <li key={`${overlap.firstId}-${overlap.secondId}`}>
            «{overlap.firstName}» ({formatTime(overlap.firstStart)}–{formatTime(overlap.firstEnd)})
            y «{overlap.secondName}» ({formatTime(overlap.secondStart)}–
            {formatTime(overlap.secondEnd)}) se solapan el {formatDayLabel(overlap.date)}.
          </li>
        ))}
        {outsideDates.map((outside) => (
          <li key={outside.activityId}>
            {`«${outside.name}» está fuera de las fechas del viaje (${formatTripDate(outside.date)}).`}
            {canEdit && (
              <>
                {' '}
                <Link
                  to={`/trips/${tripId}/activities/${outside.activityId}/edit`}
                  className="alert-link"
                  aria-label={`Editar «${outside.name}»`}
                >
                  Editar
                </Link>
              </>
            )}
          </li>
        ))}
        {budget && (
          <li>
            {`El coste total de las actividades (${formatBudget(budget.totalCost)}) supera el presupuesto (${formatBudget(budget.budget)}).`}
          </li>
        )}
      </ul>
    </Alert>
  );
}
