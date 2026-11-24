import { render, screen } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import { spendingOf } from '../testHelpers';
import SpendingSection from './SpendingSection';

const EMPTY_MESSAGE = 'Aún no hay gastos. Añade un coste a una actividad para ver los gráficos.';

describe('I5 SpendingSection', () => {
  test('shows a loading text while the data has not arrived', () => {
    render(<SpendingSection failed={false} />);

    expect(screen.getByRole('heading', { name: 'Gastos' })).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('Cargando gastos…');
  });

  test('says it could not load the data when the server failed and there is nothing to show', () => {
    render(<SpendingSection failed />);

    expect(screen.getByRole('alert')).toHaveTextContent('No se pudieron cargar los gastos.');
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  test('invites to add a cost when no activity has one', () => {
    render(<SpendingSection spending={spendingOf({ budget: 500 })} failed={false} />);

    expect(screen.getByText(EMPTY_MESSAGE)).toBeInTheDocument();
    expect(screen.queryByText('Gasto por categoría')).not.toBeInTheDocument();
  });

  test('shows the spending by category once there are costs', () => {
    render(
      <SpendingSection
        spending={spendingOf({
          totalCost: 30,
          byCategory: [{ category: 'FOOD', total: 30 }],
        })}
        failed={false}
      />,
    );

    expect(screen.getByText('Gasto por categoría', { selector: 'figcaption' })).toBeInTheDocument();
    expect(screen.queryByText(EMPTY_MESSAGE)).not.toBeInTheDocument();
  });

  test('keeps showing the old data when a later reload fails', () => {
    render(
      <SpendingSection
        spending={spendingOf({ totalCost: 30, byCategory: [{ category: 'FOOD', total: 30 }] })}
        failed
      />,
    );

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(screen.getByText('Gasto por categoría', { selector: 'figcaption' })).toBeInTheDocument();
  });
});
