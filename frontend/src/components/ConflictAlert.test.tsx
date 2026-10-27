import { screen } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import { renderRoutes } from '../renderHelpers';
import { conflictsOf } from '../testHelpers';
import type { TripConflicts } from '../types';
import { formatBudget } from '../utils/tripFormat';
import ConflictAlert from './ConflictAlert';

function show(conflicts?: TripConflicts, canEdit = true) {
  return renderRoutes(
    [
      {
        path: '/trips/:tripId',
        element: <ConflictAlert tripId={4} conflicts={conflicts} canEdit={canEdit} />,
      },
    ],
    '/trips/4',
  );
}

const overlap = {
  date: '2027-07-11',
  firstId: 1,
  firstName: 'Visita',
  firstStart: '10:00:00',
  firstEnd: '12:00:00',
  secondId: 2,
  secondName: 'Comida',
  secondStart: '11:30:00',
  secondEnd: '13:00:00',
};

describe('B6 ConflictAlert', () => {
  test('shows nothing before the conflicts arrive', () => {
    show(undefined);

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  test('shows nothing when there are no conflicts', () => {
    show(conflictsOf());

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  test('says which activities overlap and on which day', () => {
    show(conflictsOf({ overlaps: [overlap] }));

    const alert = screen.getByRole('alert');
    expect(alert).toHaveTextContent('Conflictos en el itinerario');
    expect(alert).toHaveTextContent(
      '«Visita» (10:00–12:00) y «Comida» (11:30–13:00) se solapan el dom, 11 jul.',
    );
  });

  test('lists every overlap', () => {
    show(
      conflictsOf({
        overlaps: [overlap, { ...overlap, firstId: 1, secondId: 3, secondName: 'Paseo' }],
      }),
    );

    expect(screen.getAllByRole('listitem')).toHaveLength(2);
  });

  test('names the activities outside the trip dates with a link to edit each one', () => {
    show(
      conflictsOf({
        outsideDates: [
          { activityId: 8, name: 'Museo', date: '2027-07-20' },
          { activityId: 9, name: 'Playa', date: '2027-07-01' },
        ],
      }),
    );

    expect(screen.getByRole('alert')).toHaveTextContent(
      '«Museo» está fuera de las fechas del viaje (20 jul 2027).',
    );
    expect(screen.getByRole('link', { name: 'Editar «Museo»' })).toHaveAttribute(
      'href',
      '/trips/4/activities/8/edit',
    );
    expect(screen.getByRole('link', { name: 'Editar «Playa»' })).toHaveAttribute(
      'href',
      '/trips/4/activities/9/edit',
    );
  });

  test('gives the edit links only to whoever can edit', () => {
    show(
      conflictsOf({ outsideDates: [{ activityId: 8, name: 'Museo', date: '2027-07-20' }] }),
      false,
    );

    expect(screen.getByRole('alert')).toHaveTextContent('«Museo» está fuera');
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });

  test('compares the total cost with the budget', () => {
    show(conflictsOf({ budget: { totalCost: 1250.5, budget: 1000 } }));

    // The currency format has non-breaking spaces; the matcher normalises the element's text only.
    const expected = `El coste total de las actividades (${formatBudget(1250.5)}) supera el presupuesto (${formatBudget(1000)}).`;
    expect(screen.getByRole('alert')).toHaveTextContent(expected.replace(/\s/g, ' '));
  });

  test('shows the three kinds together', () => {
    show(
      conflictsOf({
        overlaps: [overlap],
        outsideDates: [{ activityId: 8, name: 'Museo', date: '2027-07-20' }],
        budget: { totalCost: 120, budget: 100 },
      }),
    );

    expect(screen.getAllByRole('listitem')).toHaveLength(3);
  });
});
