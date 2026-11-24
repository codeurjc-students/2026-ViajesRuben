import { render, screen, within } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import { spendingOf } from '../testHelpers';
import SpendingByCategoryChart from './SpendingByCategoryChart';

const byCategory = [
  { category: 'ACCOMMODATION' as const, total: 1000 },
  { category: 'FOOD' as const, total: 150.5 },
  { category: null, total: 20 },
];

/** Amounts are written with a non-breaking space before the euro sign. */
const text = (element: HTMLElement) => element.textContent?.replaceAll(' ', ' ');

describe('I5 SpendingByCategoryChart', () => {
  test('lists every category in Spanish with its amount in euros, "Sin categoría" included', () => {
    render(
      <SpendingByCategoryChart
        spending={spendingOf({ budget: 2000, totalCost: 1170.5, byCategory })}
      />,
    );

    const table = screen.getByRole('table', { name: 'Gasto por categoría', hidden: true });
    const rows = within(table).getAllByRole('row', { hidden: true }).slice(1);
    expect(rows.map((row) => text(row))).toEqual([
      'Alojamiento1000,00 €',
      'Comida150,50 €',
      'Sin categoría20,00 €',
    ]);
  });

  test('draws a bar per category', () => {
    const { container } = render(
      <SpendingByCategoryChart spending={spendingOf({ totalCost: 1170.5, byCategory })} />,
    );

    expect(container.querySelectorAll('.recharts-bar-rectangle')).toHaveLength(3);
  });

  test('shows the total against the budget with a bar that is not over', () => {
    render(
      <SpendingByCategoryChart
        spending={spendingOf({ budget: 2000, totalCost: 1170.5, byCategory })}
      />,
    );

    expect(text(screen.getByText(/Gastado/, { selector: 'p' }))).toBe(
      'Gastado 1170,50 € de 2000,00 €',
    );
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '58.5');
    expect(screen.queryByText('Presupuesto superado')).not.toBeInTheDocument();
  });

  test('says the budget is exceeded, by how much, when the total goes over it', () => {
    render(
      <SpendingByCategoryChart
        spending={spendingOf({ budget: 1000, totalCost: 1170.5, byCategory })}
      />,
    );

    expect(screen.getByText('Presupuesto superado')).toBeInTheDocument();
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '100');
    expect(text(screen.getByText(/Te pasas/))).toBe('Te pasas 170,50 € del presupuesto.');
  });

  test('a total equal to the budget is not over it', () => {
    render(
      <SpendingByCategoryChart
        spending={spendingOf({ budget: 1170.5, totalCost: 1170.5, byCategory })}
      />,
    );

    expect(screen.queryByText('Presupuesto superado')).not.toBeInTheDocument();
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '100');
  });

  test('without a budget it only shows the total spent, with no bar against a budget', () => {
    render(<SpendingByCategoryChart spending={spendingOf({ totalCost: 1170.5, byCategory })} />);

    expect(text(screen.getByText(/Total gastado/))).toBe('Total gastado: 1170,50 €');
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
    expect(screen.queryByText('Presupuesto superado')).not.toBeInTheDocument();
  });

  test('a budget of zero is exceeded by any cost', () => {
    render(
      <SpendingByCategoryChart
        spending={spendingOf({ budget: 0, totalCost: 20, byCategory: [byCategory[2]] })}
      />,
    );

    expect(screen.getByText('Presupuesto superado')).toBeInTheDocument();
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '100');
  });

  test('hides the drawing from assistive technology and leaves the table instead', () => {
    const { container } = render(
      <SpendingByCategoryChart spending={spendingOf({ totalCost: 20, byCategory })} />,
    );

    expect(container.querySelector('[aria-hidden="true"] .recharts-wrapper')).not.toBeNull();
    expect(screen.getByRole('table', { hidden: true })).toHaveClass('visually-hidden');
  });
});
