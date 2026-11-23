import { useCallback, useState } from 'react';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import { usePagedList } from '../hooks/usePagedList';
import { getActivityLog } from '../services/activityLogService';
import type { ActivityLogAction, ActivityLogEntry } from '../types';
import { formatDateTime, formatRelativeTime } from '../utils/tripFormat';
import './ActivityLogSection.css';

/** How many entries show at first, and what "Ver menos" goes back to. */
const COLLAPSED = 5;
/** How many more entries each "Ver más" shows. */
const STEP = 10;

const VERBS: Record<ActivityLogAction, string> = {
  CREATED: 'añadió',
  UPDATED: 'editó',
  DELETED: 'eliminó',
};

const FIELD_NAMES: Record<string, string> = {
  name: 'nombre',
  description: 'descripción',
  location: 'lugar',
  date: 'fecha',
  startTime: 'hora de inicio',
  endTime: 'hora de fin',
  cost: 'coste',
  category: 'categoría',
  image: 'imagen',
};

function changes(entry: ActivityLogEntry): string {
  return entry.changedFields.map((field) => FIELD_NAMES[field] ?? field).join(', ');
}

/**
 * The trip's change log (I4), newest first: who added, edited or deleted which activity, and when.
 * It shows the 5 latest entries, one short line each, so it never pushes the page down; "Ver más"
 * shows 10 more (asking the server for the next page when they are not loaded yet) and "Ver menos"
 * goes back to 5. Every member of the trip reads it, and so does the administrator reviewing it.
 * The parent gives a new `key` to read it again after a change made on the same page.
 */
export default function ActivityLogSection({ tripId }: Readonly<{ tripId: number }>) {
  const fetchPage = useCallback((page: number) => getActivityLog(tripId, page), [tripId]);
  const { items, totalElements, loaded, loading, last, error, loadMore } = usePagedList(fetchPage);
  const [visible, setVisible] = useState(COLLAPSED);

  function showMore() {
    const next = visible + STEP;
    setVisible(next);
    if (items.length < next && !last) loadMore();
  }

  return (
    <Card as="section" className="activity-log" aria-labelledby="activity-log-title">
      <h2 id="activity-log-title" className="h5 activity-log__title">
        Historial de cambios
      </h2>
      {error ? (
        <p className="activity-log__empty" role="alert">
          No se pudo cargar el historial de cambios.
        </p>
      ) : null}
      {!error && !loaded && <output className="d-block">Cargando historial…</output>}
      {loaded && items.length === 0 && <p className="activity-log__empty">Aún no hay cambios</p>}
      {items.length > 0 && (
        <ul className="activity-log__list">
          {items.slice(0, visible).map((entry) => (
            <li key={entry.id} className="activity-log__entry">
              <p className="activity-log__line">
                <span className="activity-log__who">{entry.userName}</span> {VERBS[entry.action]} «
                {entry.activityName}» ·{' '}
                <time dateTime={entry.createdAt} title={formatDateTime(entry.createdAt)}>
                  {formatRelativeTime(entry.createdAt)}
                </time>
              </p>
              {entry.action === 'UPDATED' && entry.changedFields.length > 0 && (
                <p className="activity-log__fields">Cambios: {changes(entry)}</p>
              )}
            </li>
          ))}
        </ul>
      )}
      {loaded && (totalElements > visible || visible > COLLAPSED) && (
        <div className="activity-log__more">
          {totalElements > visible && (
            <Button variant="link" size="sm" disabled={loading} onClick={showMore}>
              Ver más
            </Button>
          )}
          {visible > COLLAPSED && (
            <Button variant="link" size="sm" onClick={() => setVisible(COLLAPSED)}>
              Ver menos
            </Button>
          )}
        </div>
      )}
    </Card>
  );
}
