import { render, screen, within } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import { spendingOf } from '../testHelpers';
import type { DaySpending } from '../types';
import CumulativeSpendingChart from './CumulativeSpendingChart';

const byDay: DaySpending[] = [
  { date: '2027-07-10', total: 35.5, cumulative: 35.5 },
  { date: '2027-07-11', total: 0, cumulative: 35.5 },
  { date: '2027-07-12', total: 104.5, cumulative: 140 },
];

/** A trip of that many days from 1 January, each day costing 1 €. */
function longTrip(days: number): DaySpending[] {
  return Array.from({ length: days }, (_, index) => ({
    date: new Date(Date.UTC(2027, 0, 1 + index)).toISOString().slice(0, 10),
    total: 1,
    cumulative: index + 1,
  }));
}

/** Amounts are written with a non-breaking space before the euro sign. */
const text = (element: HTMLElement) => element.textContent?.replaceAll(' ', ' ');

describe('I5 CumulativeSpendingChart', () => {
  test('draws the line with a point for every day of the trip, the days without cost included', () => {
    const { container } = render(
      <CumulativeSpendingChart spending={spendingOf({ totalCost: 140, byDay })} />,
    );

    expect(container.querySelector('.recharts-line-curve')).not.toBeNull();
    expect(container.querySelectorAll('.recharts-line-dot')).toHaveLength(3);
  });

  test('gives every day, with what was spent and the running sum, in a table for screen readers', () => {
    render(<CumulativeSpendingChart spending={spendingOf({ totalCost: 140, byDay })} />);

    const table = screen.getByRole('table', { name: 'Gasto acumulado por día', hidden: true });
    const rows = within(table).getAllByRole('row', { hidden: true }).slice(1);
    expect(rows.map((row) => text(row)?.replace(/\./g, ''))).toEqual([
      '10 jul35,50 €35,50 €',
      '11 jul0,00 €35,50 €',
      '12 jul104,50 €140,00 €',
    ]);
    expect(table).toHaveClass('visually-hidden');
  });

  test('draws the budget as a reference line when the trip has one', () => {
    const { container } = render(
      <CumulativeSpendingChart spending={spendingOf({ budget: 200, totalCost: 140, byDay })} />,
    );

    expect(container.querySelectorAll('.recharts-reference-line')).toHaveLength(1);
    // Recharts draws the label of a reference line in a layer of its own, not inside the line.
    expect(container).toHaveTextContent('Presupuesto');
  });

  test('draws no budget line when the trip has no budget', () => {
    const { container } = render(
      <CumulativeSpendingChart spending={spendingOf({ budget: null, totalCost: 140, byDay })} />,
    );

    expect(container.querySelector('.recharts-reference-line')).toBeNull();
    expect(container).not.toHaveTextContent('Presupuesto');
  });

  test('a budget of zero is still a budget line', () => {
    const { container } = render(
      <CumulativeSpendingChart spending={spendingOf({ budget: 0, totalCost: 140, byDay })} />,
    );

    expect(container.querySelectorAll('.recharts-reference-line')).toHaveLength(1);
  });

  test('a one-day trip has one point', () => {
    const { container } = render(
      <CumulativeSpendingChart spending={spendingOf({ totalCost: 9.99, byDay: [byDay[0]] })} />,
    );

    expect(container.querySelectorAll('.recharts-line-dot')).toHaveLength(1);
  });

  test('draws only the line, without a dot per day, on a long trip', () => {
    const { container } = render(
      <CumulativeSpendingChart spending={spendingOf({ totalCost: 40, byDay: longTrip(40) })} />,
    );

    expect(container.querySelector('.recharts-line-curve')).not.toBeNull();
    expect(container.querySelectorAll('.recharts-line-dot')).toHaveLength(0);
    // The table still has every day.
    expect(screen.getAllByRole('row', { hidden: true })).toHaveLength(41);
  });

  test('hides the drawing from assistive technology and names the figure', () => {
    const { container } = render(
      <CumulativeSpendingChart spending={spendingOf({ totalCost: 140, byDay })} />,
    );

    expect(container.querySelector('[aria-hidden="true"] .recharts-wrapper')).not.toBeNull();
    expect(
      screen.getByRole('figure', { name: 'Gasto acumulado por día', hidden: true }),
    ).toBeInTheDocument();
  });
});
