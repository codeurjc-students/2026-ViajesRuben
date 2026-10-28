import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { ApiError } from '../services/apiError';
import { getExampleTrips } from '../services/exampleService';
import { exampleOf, pageOf, tripOf } from '../testHelpers';
import ExampleTrips from './ExampleTrips';

vi.mock('../services/exampleService', () => ({
  EXAMPLES_PAGE_SIZE: 3,
  getExampleTrips: vi.fn(),
}));

function named(id: number) {
  return exampleOf({ trip: tripOf({ id, name: `Ejemplo ${id}`, budget: null }) });
}

/** Four examples in two pages of three, as the sample data gives. */
function twoPages(page = 0) {
  return page === 0
    ? pageOf([named(1), named(2), named(3)], { size: 3, totalElements: 4, last: false })
    : pageOf([named(4)], { page: 1, size: 3, totalElements: 4, last: true });
}

beforeEach(() => {
  vi.mocked(getExampleTrips).mockReset();
});

function renderExamples() {
  return render(
    <MemoryRouter>
      <ExampleTrips />
    </MemoryRouter>,
  );
}

describe('B1 ExampleTrips', () => {
  test('shows the first three examples with the section title and subtitle', async () => {
    vi.mocked(getExampleTrips).mockImplementation((page = 0) => Promise.resolve(twoPages(page)));

    renderExamples();

    expect(screen.getByRole('status')).toHaveTextContent('Cargando itinerarios de ejemplo');
    expect(await screen.findByRole('heading', { name: 'Ejemplo 1' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Itinerarios de ejemplo' })).toBeInTheDocument();
    expect(
      screen.getByText(
        'Viajes de muestra hechos con TripMate. Ábrelos para ver el itinerario día a día.',
      ),
    ).toBeInTheDocument();
    expect(screen.getAllByRole('article')).toHaveLength(3);
    expect(getExampleTrips).toHaveBeenCalledWith(0);
  });

  test('next shows the following page and previous goes back', async () => {
    vi.mocked(getExampleTrips).mockImplementation((page = 0) => Promise.resolve(twoPages(page)));
    renderExamples();
    await screen.findByRole('heading', { name: 'Ejemplo 1' });
    const previous = screen.getByRole('button', { name: 'Ejemplos anteriores' });
    const next = screen.getByRole('button', { name: 'Ejemplos siguientes' });
    expect(previous).toBeDisabled();
    expect(next).toBeEnabled();

    fireEvent.click(next);

    expect(await screen.findByRole('heading', { name: 'Ejemplo 4' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Ejemplo 1' })).not.toBeInTheDocument();
    expect(getExampleTrips).toHaveBeenLastCalledWith(1);
    expect(previous).toBeEnabled();
    expect(next).toBeDisabled();

    fireEvent.click(previous);

    expect(await screen.findByRole('heading', { name: 'Ejemplo 1' })).toBeInTheDocument();
  });

  test('keeps the buttons while the next page loads', async () => {
    vi.mocked(getExampleTrips).mockImplementation((page = 0) =>
      page === 0 ? Promise.resolve(twoPages(0)) : new Promise(() => {}),
    );
    renderExamples();
    await screen.findByRole('heading', { name: 'Ejemplo 1' });

    fireEvent.click(screen.getByRole('button', { name: 'Ejemplos siguientes' }));

    expect(await screen.findByRole('status')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ejemplos anteriores' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Ejemplos siguientes' })).toBeDisabled();
  });

  test('has no buttons when everything fits in one page', async () => {
    vi.mocked(getExampleTrips).mockResolvedValue(
      pageOf([named(1), named(2)], { size: 3, totalElements: 2 }),
    );

    renderExamples();

    await screen.findByRole('heading', { name: 'Ejemplo 1' });
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  test('says so when there are no examples yet', async () => {
    vi.mocked(getExampleTrips).mockResolvedValue(pageOf([], { size: 3 }));

    renderExamples();

    expect(await screen.findByText('Todavía no hay itinerarios de ejemplo.')).toBeInTheDocument();
    expect(screen.queryByRole('article')).not.toBeInTheDocument();
  });

  test('a failure shows the route error page', async () => {
    vi.mocked(getExampleTrips).mockRejectedValue(new ApiError(500));
    const router = createMemoryRouter(
      [{ path: '/', element: <ExampleTrips />, errorElement: <p>Página de error</p> }],
      { initialEntries: ['/'] },
    );

    render(<RouterProvider router={router} />);

    await waitFor(() => expect(screen.getByText('Página de error')).toBeInTheDocument());
  });
});
