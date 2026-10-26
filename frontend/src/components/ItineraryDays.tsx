import { useEffect, useRef } from 'react';
import Button from 'react-bootstrap/Button';
import Nav from 'react-bootstrap/Nav';
import { dayNumber } from '../utils/tripDays';
import { formatDayLabel } from '../utils/tripFormat';
import './ItineraryDays.css';

interface ItineraryDaysProps {
  /** Every day of the trip, in order. */
  days: string[];
  selected: string;
  onSelect: (day: string) => void;
}

/** Previous and next buttons around one tab per day of the trip; the strip scrolls on narrow screens. */
export default function ItineraryDays({ days, selected, onSelect }: Readonly<ItineraryDaysProps>) {
  const index = days.indexOf(selected);
  const selectedTab = useRef<HTMLButtonElement>(null);

  // Keeps the chosen day visible when the strip is wider than the screen.
  useEffect(() => {
    selectedTab.current?.scrollIntoView?.({ block: 'nearest', inline: 'center' });
  }, [selected]);

  return (
    <div className="itinerary-days">
      <Button
        variant="outline-secondary"
        size="sm"
        aria-label="Día anterior"
        disabled={index <= 0}
        onClick={() => onSelect(days[index - 1])}
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 5l-7 7 7 7" />
        </svg>
      </Button>
      <Nav
        variant="pills"
        role="tablist"
        aria-label="Días del viaje"
        className="itinerary-days__tabs"
      >
        {days.map((day) => (
          <Nav.Item key={day} role="presentation">
            <Nav.Link
              as="button"
              type="button"
              role="tab"
              ref={day === selected ? selectedTab : undefined}
              active={day === selected}
              aria-selected={day === selected}
              onClick={() => onSelect(day)}
            >
              <span className="itinerary-days__number">{`Día ${dayNumber(day, days[0])}`}</span>
              <span className="itinerary-days__date">{formatDayLabel(day)}</span>
            </Nav.Link>
          </Nav.Item>
        ))}
      </Nav>
      <Button
        variant="outline-secondary"
        size="sm"
        aria-label="Día siguiente"
        disabled={index < 0 || index >= days.length - 1}
        onClick={() => onSelect(days[index + 1])}
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M9 5l7 7-7 7" />
        </svg>
      </Button>
    </div>
  );
}
