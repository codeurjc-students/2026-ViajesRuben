import { useCallback } from 'react';
import { usePagedList } from '../hooks/usePagedList';
import { getActivities } from '../services/activityService';
import type { Trip } from '../types';
import { formatDayLabel } from '../utils/tripFormat';
import ActivityCard from './ActivityCard';
import LoadMoreButton from './LoadMoreButton';

interface DayActivitiesProps {
  trip: Trip;
  /** The day to list, yyyy-MM-dd. The parent gives a new `key` per day, so each day starts clean. */
  day: string;
  isCreator: boolean;
  onActivityDeleted?: () => void;
}

/** The activities of one day, in time order, ten at a time. */
export default function DayActivities({
  trip,
  day,
  isCreator,
  onActivityDeleted,
}: Readonly<DayActivitiesProps>) {
  const fetchPage = useCallback(
    (page: number) => getActivities(trip.id, day, page),
    [trip.id, day],
  );
  const { items, totalElements, loaded, loading, last, error, loadMore, reload } =
    usePagedList(fetchPage);

  // Rethrown so the route's error page (inside the layout) is shown.
  if (error) throw error;

  return (
    <div>
      <h3 className="h5 mb-3">
        {formatDayLabel(day)}
        {loaded && totalElements > 0 && (
          <span className="itinerary__count">
            {totalElements === 1 ? '1 actividad' : `${totalElements} actividades`}
          </span>
        )}
      </h3>
      {!loaded && <p role="status">Cargando actividades…</p>}
      {loaded && items.length === 0 && (
        <p className="itinerary__empty">
          {isCreator
            ? 'No hay actividades este día. Añade la primera con «Nueva actividad».'
            : 'No hay actividades este día.'}
        </p>
      )}
      {items.length > 0 && (
        <ol className="itinerary__stops">
          {items.map((activity) => (
            <ActivityCard
              key={activity.id}
              activity={activity}
              canEdit={isCreator}
              onDeleted={() => {
                reload();
                onActivityDeleted?.();
              }}
            />
          ))}
        </ol>
      )}
      {loaded && <LoadMoreButton last={last} loading={loading} onClick={loadMore} />}
    </div>
  );
}
