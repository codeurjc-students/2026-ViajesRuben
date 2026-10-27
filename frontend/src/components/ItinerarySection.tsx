import { useMemo } from 'react';
import Card from 'react-bootstrap/Card';
import { Link, useSearchParams } from 'react-router';
import { useAuth } from '../hooks/useAuth';
import type { Trip } from '../types';
import { tripDays } from '../utils/tripDays';
import DayActivities from './DayActivities';
import ItineraryDays from './ItineraryDays';
import './ItinerarySection.css';

/**
 * The trip's itinerary, one day at a time. The selected day is in the URL (`?day=yyyy-MM-dd`), so
 * coming back from the activity form lands on the same day and the browser's Back button works.
 */
interface ItinerarySectionProps {
  trip: Trip;
  /** Called after an activity was deleted, e.g. to refresh the trip's conflicts. */
  onActivityDeleted?: () => void;
}

export default function ItinerarySection({
  trip,
  onActivityDeleted,
}: Readonly<ItinerarySectionProps>) {
  const { user } = useAuth();
  const [params, setParams] = useSearchParams();
  const days = useMemo(
    () => tripDays(trip.startDate, trip.endDate),
    [trip.startDate, trip.endDate],
  );
  const requested = params.get('day');
  const day = requested && days.includes(requested) ? requested : days[0];
  const isCreator = user?.id === trip.creatorId;

  return (
    <Card as="section" className="itinerary" aria-labelledby="itinerary-title">
      <div className="itinerary__header">
        <h2 id="itinerary-title" className="h3 mb-0">
          Itinerario
        </h2>
        {isCreator && (
          <Link to={`/trips/${trip.id}/activities/new?day=${day}`} className="btn btn-primary">
            Nueva actividad
          </Link>
        )}
      </div>
      <ItineraryDays days={days} selected={day} onSelect={(next) => setParams({ day: next })} />
      <DayActivities
        key={day}
        trip={trip}
        day={day}
        isCreator={isCreator}
        onActivityDeleted={onActivityDeleted}
      />
    </Card>
  );
}
