import { useMemo } from 'react';
import Card from 'react-bootstrap/Card';
import { Link, useSearchParams } from 'react-router';
import { getPublicActivities } from '../services/publicTripService';
import type { Trip } from '../types';
import { tripDays } from '../utils/tripDays';
import DayActivities from './DayActivities';
import ItineraryDays from './ItineraryDays';
import './ItinerarySection.css';

/** What the itinerary needs of a trip. */
type ItineraryTrip = Pick<Trip, 'id' | 'startDate' | 'endDate'>;

interface ItinerarySectionProps {
  trip: ItineraryTrip;
  /**
   * Whether the user may add, edit and delete activities: the trip's creator and its participants
   * (I4), which the page that shows the trip has already worked out. Anyone else only reads.
   */
  canEdit?: boolean;
  /** Called after an activity was deleted, e.g. to refresh the trip's conflicts. */
  onActivityDeleted?: () => void;
  /** A public itinerary: read from the public endpoints and never editable, not even by its creator. */
  publicPreview?: boolean;
}

/**
 * The trip's itinerary, one day at a time. The selected day is in the URL (`?day=yyyy-MM-dd`), so
 * coming back from the activity form lands on the same day and the browser's Back button works.
 */
export default function ItinerarySection({
  trip,
  canEdit = false,
  onActivityDeleted,
  publicPreview = false,
}: Readonly<ItinerarySectionProps>) {
  const [params, setParams] = useSearchParams();
  const days = useMemo(
    () => tripDays(trip.startDate, trip.endDate),
    [trip.startDate, trip.endDate],
  );
  const requested = params.get('day');
  const day = requested && days.includes(requested) ? requested : days[0];
  const editable = canEdit && !publicPreview;

  return (
    <Card as="section" className="itinerary" aria-labelledby="itinerary-title">
      <div className="itinerary__header">
        <h2 id="itinerary-title" className="h3 mb-0">
          Itinerario
        </h2>
        {editable && (
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
        canEdit={editable}
        onActivityDeleted={onActivityDeleted}
        load={publicPreview ? getPublicActivities : undefined}
      />
    </Card>
  );
}
