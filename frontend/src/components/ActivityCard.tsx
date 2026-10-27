import { useState } from 'react';
import Button from 'react-bootstrap/Button';
import { Link } from 'react-router';
import type { Activity } from '../types';
import { formatBudget, formatDuration, formatTime } from '../utils/tripFormat';
import CategoryName from './CategoryName';
import DeleteActivityModal from './DeleteActivityModal';
import StoredImage from './StoredImage';
import './ActivityCard.css';

interface ActivityCardProps {
  activity: Activity;
  /** The creator of the trip may edit and delete; anyone else only reads. */
  canEdit: boolean;
  /** Called after the activity was deleted from the card, so the list can reload. */
  onDeleted: () => void;
}

function schedule(activity: Activity): string {
  if (!activity.startTime) return 'Todo el día';
  if (!activity.endTime) return formatTime(activity.startTime);
  return `${formatTime(activity.startTime)} – ${formatTime(activity.endTime)}`;
}

/** One stop of the day's line: schedule, name, details, image and, for the creator, edit and delete. */
export default function ActivityCard({
  activity,
  canEdit,
  onDeleted,
}: Readonly<ActivityCardProps>) {
  const [confirming, setConfirming] = useState(false);

  return (
    <li className="activity-card">
      <div className="activity-card__body">
        <p className="activity-card__time">
          <span>{schedule(activity)}</span>
          {activity.startTime && activity.endTime && (
            <span className="activity-card__duration">
              {formatDuration(activity.startTime, activity.endTime)}
            </span>
          )}
        </p>
        <h4 className="activity-card__name">{activity.name}</h4>
        <p className="activity-card__meta">
          {activity.category && <CategoryName category={activity.category} />}
          {activity.category && activity.location && ' — '}
          {activity.location}
        </p>
        {activity.cost !== null && (
          <p className="activity-card__cost">{formatBudget(activity.cost)}</p>
        )}
        {activity.description && (
          <p className="activity-card__description">{activity.description}</p>
        )}
        {canEdit && (
          <div className="activity-card__actions">
            <Link
              to={`/trips/${activity.tripId}/activities/${activity.id}/edit`}
              className="btn btn-outline-primary btn-sm"
              aria-label={`Editar ${activity.name}`}
            >
              Editar
            </Link>
            <Button
              variant="outline-danger"
              size="sm"
              aria-label={`Eliminar ${activity.name}`}
              onClick={() => setConfirming(true)}
            >
              Eliminar
            </Button>
          </div>
        )}
      </div>
      {activity.imageId && (
        <div className="activity-card__image">
          <StoredImage id={activity.imageId} alt={`Imagen de ${activity.name}`} fallback={null} />
        </div>
      )}
      {canEdit && (
        <DeleteActivityModal
          activity={activity}
          show={confirming}
          onCancel={() => setConfirming(false)}
          onDeleted={() => {
            setConfirming(false);
            onDeleted();
          }}
        />
      )}
    </li>
  );
}
