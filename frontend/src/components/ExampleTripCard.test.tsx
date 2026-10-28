import { screen, within } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import { renderAsUser } from '../renderHelpers';
import { exampleOf, tripOf } from '../testHelpers';
import ExampleTripCard from './ExampleTripCard';

describe('B1 ExampleTripCard', () => {
  test('shows the name, the destination, the days and the budget', () => {
    renderAsUser(<ExampleTripCard example={exampleOf()} />, null);

    expect(screen.getByRole('heading', { name: 'Roma en cinco días' })).toBeInTheDocument();
    expect(screen.getByText(/^Roma · 5 días · 1200,00 €$/)).toBeInTheDocument();
  });

  test('previews the first activity of each of the first days', () => {
    renderAsUser(<ExampleTripCard example={exampleOf()} />, null);

    const items = within(screen.getByRole('list', { name: 'Primeros días' })).getAllByRole(
      'listitem',
    );
    expect(items).toHaveLength(2);
    expect(items[0]).toHaveTextContent('Día 1 · Coliseo');
    expect(items[1]).toHaveTextContent('Día 2 · Vaticano');
  });

  test('links to the full itinerary of the example', () => {
    renderAsUser(<ExampleTripCard example={exampleOf()} />, null);

    const link = screen.getByRole('link', {
      name: 'Ver itinerario completo de Roma en cinco días',
    });
    expect(link).toHaveAttribute('href', '/examples/10');
    expect(link).toHaveTextContent('Ver itinerario completo');
  });

  test('has no edit or delete buttons, not even for the logged-in creator', () => {
    const example = exampleOf({ trip: tripOf({ id: 10, creatorId: 1, budget: 500 }) });
    renderAsUser(<ExampleTripCard example={example} />);

    expect(screen.queryByRole('button')).not.toBeInTheDocument();
    // The only link is the one that opens the example.
    expect(screen.getAllByRole('link')).toHaveLength(1);
  });

  test('leaves out the budget and the preview when there are none', () => {
    const example = exampleOf({
      trip: tripOf({
        id: 10,
        destination: 'Oporto',
        startDate: '2027-03-12',
        endDate: '2027-03-12',
      }),
      firstDays: [],
    });
    renderAsUser(<ExampleTripCard example={example} />, null);

    expect(screen.getByText('Oporto · 1 día')).toBeInTheDocument();
    expect(screen.queryByRole('list')).not.toBeInTheDocument();
  });

  test('describes the cover of the trip, and the default cover has no alt text', () => {
    const { container, unmount } = renderAsUser(
      <ExampleTripCard
        example={exampleOf({ trip: tripOf({ id: 10, coverImageId: 'abc', budget: null }) })}
      />,
      null,
    );

    expect(screen.getByAltText('Portada de Escapada')).toHaveAttribute(
      'src',
      expect.stringContaining('/api/v1/images/abc'),
    );
    unmount();

    const withoutCover = renderAsUser(<ExampleTripCard example={exampleOf()} />, null);
    expect(withoutCover.container.querySelector('img')).toHaveAttribute('alt', '');
    expect(container).toBeDefined();
  });
});
