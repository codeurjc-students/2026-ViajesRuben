import { screen, within } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import { renderAsUser } from '../renderHelpers';
import { publicTripOf } from '../testHelpers';
import PublicTripCard from './PublicTripCard';

function withTrip(overrides: Partial<ReturnType<typeof publicTripOf>['trip']>) {
  const base = publicTripOf();
  return { ...base, trip: { ...base.trip, ...overrides } };
}

describe('B1 I7 PublicTripCard', () => {
  test('shows the name, the destination, the days and the budget', () => {
    renderAsUser(<PublicTripCard summary={publicTripOf()} />, null);

    expect(screen.getByRole('heading', { name: 'Roma en cinco días' })).toBeInTheDocument();
    expect(screen.getByText(/^Roma · 5 días · 1200,00 €$/)).toBeInTheDocument();
  });

  test('shows the name of the author, never an email', () => {
    renderAsUser(<PublicTripCard summary={publicTripOf()} />, null);

    expect(screen.getByText('Por Carlos')).toBeInTheDocument();
    expect(screen.queryByText(/@/)).not.toBeInTheDocument();
  });

  test('previews the first activity of each of the first days', () => {
    renderAsUser(<PublicTripCard summary={publicTripOf()} />, null);

    const items = within(screen.getByRole('list', { name: 'Primeros días' })).getAllByRole(
      'listitem',
    );
    expect(items).toHaveLength(2);
    expect(items[0]).toHaveTextContent('Día 1 · Coliseo');
    expect(items[1]).toHaveTextContent('Día 2 · Vaticano');
  });

  test('links to the public page of the itinerary', () => {
    renderAsUser(<PublicTripCard summary={publicTripOf()} />, null);

    const link = screen.getByRole('link', {
      name: 'Ver itinerario completo de Roma en cinco días',
    });
    expect(link).toHaveAttribute('href', '/public/10');
    expect(link).toHaveTextContent('Ver itinerario completo');
  });

  test('has no edit or delete buttons, whoever is logged in', () => {
    renderAsUser(<PublicTripCard summary={publicTripOf()} />);

    expect(screen.queryByRole('button')).not.toBeInTheDocument();
    // The only link is the one that opens the itinerary.
    expect(screen.getAllByRole('link')).toHaveLength(1);
  });

  test('leaves out the budget and the preview when there are none', () => {
    const summary = {
      ...withTrip({
        destination: 'Oporto',
        startDate: '2027-03-12',
        endDate: '2027-03-12',
        budget: null,
      }),
      firstDays: [],
    };
    renderAsUser(<PublicTripCard summary={summary} />, null);

    expect(screen.getByText('Oporto · 1 día')).toBeInTheDocument();
    expect(screen.queryByRole('list')).not.toBeInTheDocument();
  });

  test('describes the cover of the trip, and the default cover has no alt text', () => {
    const { unmount } = renderAsUser(
      <PublicTripCard summary={withTrip({ coverImageId: 'abc' })} />,
      null,
    );

    expect(screen.getByAltText('Portada de Roma en cinco días')).toHaveAttribute(
      'src',
      expect.stringContaining('/api/v1/images/abc'),
    );
    unmount();

    const withoutCover = renderAsUser(<PublicTripCard summary={publicTripOf()} />, null);
    expect(withoutCover.container.querySelector('img')).toHaveAttribute('alt', '');
  });
});
