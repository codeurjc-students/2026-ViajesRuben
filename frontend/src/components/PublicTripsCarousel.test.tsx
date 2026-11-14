import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { ApiError } from '../services/apiError';
import { getPublicTrips } from '../services/publicTripService';
import { pageOf, publicTripOf } from '../testHelpers';
import PublicTripsCarousel from './PublicTripsCarousel';

vi.mock('../services/publicTripService', () => ({
  PUBLIC_PAGE_SIZE: 3,
  CAROUSEL_MAX_PAGES: 3,
  getPublicTrips: vi.fn(),
}));

function named(id: number) {
  const base = publicTripOf();
  return { ...base, trip: { ...base.trip, id, name: `Público ${id}`, budget: null } };
}

/** `count` trips in pages of three, as the backend pages them, `total` in the whole list. */
function pagesOf(total: number, page: number) {
  const first = page * 3 + 1;
  const count = Math.max(0, Math.min(3, total - page * 3));
  return pageOf(
    Array.from({ length: count }, (_, i) => named(first + i)),
    { page, size: 3, totalElements: total, last: (page + 1) * 3 >= total },
  );
}

beforeEach(() => {
  vi.mocked(getPublicTrips).mockReset();
});

function renderCarousel() {
  return render(
    <MemoryRouter>
      <PublicTripsCarousel />
    </MemoryRouter>,
  );
}

describe('B1 I7 PublicTripsCarousel', () => {
  test('shows the first three public trips with the section title and subtitle', async () => {
    vi.mocked(getPublicTrips).mockImplementation((page = 0) => Promise.resolve(pagesOf(4, page)));

    renderCarousel();

    expect(screen.getByRole('status')).toHaveTextContent('Cargando itinerarios públicos');
    expect(await screen.findByRole('heading', { name: 'Público 1' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Itinerarios públicos' })).toBeInTheDocument();
    expect(
      screen.getByText(
        'Viajes publicados por otros viajeros. Ábrelos para ver el itinerario día a día.',
      ),
    ).toBeInTheDocument();
    expect(screen.getAllByRole('article')).toHaveLength(3);
    expect(getPublicTrips).toHaveBeenCalledWith(0);
  });

  test('has a "Ver todos" link next to the title that goes to the public itineraries page', async () => {
    vi.mocked(getPublicTrips).mockResolvedValue(pagesOf(2, 0));

    renderCarousel();

    await screen.findByRole('heading', { name: 'Público 1' });
    expect(screen.getByRole('link', { name: 'Ver todos' })).toHaveAttribute('href', '/public');
  });

  test('next shows the following page and previous goes back', async () => {
    vi.mocked(getPublicTrips).mockImplementation((page = 0) => Promise.resolve(pagesOf(4, page)));
    renderCarousel();
    await screen.findByRole('heading', { name: 'Público 1' });
    const previous = screen.getByRole('button', { name: 'Itinerarios anteriores' });
    const next = screen.getByRole('button', { name: 'Itinerarios siguientes' });
    expect(previous).toBeDisabled();
    expect(next).toBeEnabled();

    fireEvent.click(next);

    expect(await screen.findByRole('heading', { name: 'Público 4' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Público 1' })).not.toBeInTheDocument();
    expect(getPublicTrips).toHaveBeenLastCalledWith(1);
    expect(previous).toBeEnabled();
    expect(next).toBeDisabled();

    fireEvent.click(previous);

    expect(await screen.findByRole('heading', { name: 'Público 1' })).toBeInTheDocument();
  });

  test('stops after the third page even when there are more public trips', async () => {
    vi.mocked(getPublicTrips).mockImplementation((page = 0) => Promise.resolve(pagesOf(20, page)));
    renderCarousel();
    await screen.findByRole('heading', { name: 'Público 1' });
    const next = screen.getByRole('button', { name: 'Itinerarios siguientes' });

    fireEvent.click(next);
    await screen.findByRole('heading', { name: 'Público 4' });
    expect(next).toBeEnabled();
    fireEvent.click(next);

    expect(await screen.findByRole('heading', { name: 'Público 7' })).toBeInTheDocument();
    expect(next).toBeDisabled();
    expect(getPublicTrips).toHaveBeenCalledTimes(3);
    expect(getPublicTrips).not.toHaveBeenCalledWith(3);
  });

  test('keeps the buttons while the next page loads', async () => {
    vi.mocked(getPublicTrips).mockImplementation((page = 0) =>
      page === 0 ? Promise.resolve(pagesOf(4, 0)) : new Promise(() => {}),
    );
    renderCarousel();
    await screen.findByRole('heading', { name: 'Público 1' });

    fireEvent.click(screen.getByRole('button', { name: 'Itinerarios siguientes' }));

    expect(await screen.findByRole('status')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Itinerarios anteriores' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Itinerarios siguientes' })).toBeDisabled();
  });

  test('has no buttons when everything fits in one page', async () => {
    vi.mocked(getPublicTrips).mockResolvedValue(pagesOf(2, 0));

    renderCarousel();

    await screen.findByRole('heading', { name: 'Público 1' });
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  test('says so when there are no public trips yet', async () => {
    vi.mocked(getPublicTrips).mockResolvedValue(pageOf([], { size: 3 }));

    renderCarousel();

    expect(await screen.findByText('Todavía no hay itinerarios públicos.')).toBeInTheDocument();
    expect(screen.queryByRole('article')).not.toBeInTheDocument();
  });

  test('a failure shows the route error page', async () => {
    vi.mocked(getPublicTrips).mockRejectedValue(new ApiError(500));
    const router = createMemoryRouter(
      [{ path: '/', element: <PublicTripsCarousel />, errorElement: <p>Página de error</p> }],
      { initialEntries: ['/'] },
    );

    render(<RouterProvider router={router} />);

    await waitFor(() => expect(screen.getByText('Página de error')).toBeInTheDocument());
  });

  test('ignores an answer that arrives after the section is gone, whether it succeeds or fails', async () => {
    let succeed: (page: ReturnType<typeof pagesOf>) => void = () => {};
    let fail: (reason: unknown) => void = () => {};
    vi.mocked(getPublicTrips)
      .mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            succeed = resolve;
          }),
      )
      .mockImplementationOnce(
        () =>
          new Promise((_resolve, reject) => {
            fail = reject;
          }),
      );
    const first = renderCarousel();
    first.unmount();
    const second = renderCarousel();
    second.unmount();

    succeed(pagesOf(4, 0));
    fail(new ApiError(500));
    await Promise.resolve();

    expect(screen.queryByRole('article')).not.toBeInTheDocument();
  });
});
