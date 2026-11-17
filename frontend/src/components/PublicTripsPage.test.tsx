import { fireEvent, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import PublicTripsPage from './PublicTripsPage';
import { renderAsUser, renderRoutes } from '../renderHelpers';
import { ApiError } from '../services/apiError';
import { getPublicTrips } from '../services/publicTripService';
import { setPublication } from '../services/tripService';
import { ANA, pageOf, publicTripOf, tripOf } from '../testHelpers';

vi.mock('../services/publicTripService', () => ({
  PUBLIC_LIST_PAGE_SIZE: 10,
  getPublicTrips: vi.fn(),
}));
vi.mock('../services/tripService', () => ({ setPublication: vi.fn() }));

const ADMIN = { ...ANA, id: 99, name: 'Admin', role: 'ADMIN' as const };

const rome = publicTripOf();
const oslo = publicTripOf({
  trip: {
    ...rome.trip,
    id: 11,
    name: 'Fiordos',
    destination: 'Oslo',
    startDate: '2027-07-10',
    endDate: '2027-07-17',
    authorName: 'Ana',
  },
});

function renderPage() {
  return renderAsUser(<PublicTripsPage />, null, '/public');
}

function search(text: string) {
  fireEvent.change(screen.getByLabelText('Buscar por nombre o destino'), {
    target: { value: text },
  });
  fireEvent.click(screen.getByRole('button', { name: 'Buscar' }));
}

beforeEach(() => {
  vi.mocked(getPublicTrips).mockReset();
  vi.mocked(setPublication).mockReset();
});

describe('I10 PublicTripsPage moderation', () => {
  test.each([
    ['a visitor', null],
    ['a regular user', ANA],
  ])('%s does not see the unpublish buttons', async (_who, user) => {
    vi.mocked(getPublicTrips).mockResolvedValue(pageOf([rome, oslo]));

    renderAsUser(<PublicTripsPage />, user, '/public');

    await screen.findByRole('link', { name: 'Fiordos' });
    expect(screen.queryByRole('button', { name: /Despublicar/ })).not.toBeInTheDocument();
  });

  test('the administrator sees one button per row', async () => {
    vi.mocked(getPublicTrips).mockResolvedValue(pageOf([rome, oslo]));

    renderAsUser(<PublicTripsPage />, ADMIN, '/public');

    expect(
      await screen.findByRole('button', { name: 'Despublicar Roma en cinco días' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Despublicar Fiordos' })).toBeInTheDocument();
  });

  test('confirming unpublishes the trip, reloads the list and shows a notice', async () => {
    vi.mocked(getPublicTrips).mockResolvedValueOnce(pageOf([rome, oslo]));
    vi.mocked(getPublicTrips).mockResolvedValueOnce(pageOf([oslo]));
    vi.mocked(setPublication).mockResolvedValue(tripOf({ id: 10, published: false }));
    renderAsUser(<PublicTripsPage />, ADMIN, '/public');

    fireEvent.click(await screen.findByRole('button', { name: 'Despublicar Roma en cinco días' }));
    expect(screen.getByRole('dialog', { name: 'Despublicar itinerario' })).toHaveTextContent(
      'dejará de ser público; el viaje no se borra.',
    );
    fireEvent.click(screen.getByRole('button', { name: 'Despublicar' }));

    await waitFor(() =>
      expect(screen.queryByRole('link', { name: 'Roma en cinco días' })).not.toBeInTheDocument(),
    );
    expect(setPublication).toHaveBeenCalledWith(10, false);
    expect(screen.getByRole('link', { name: 'Fiordos' })).toBeInTheDocument();
    expect(screen.getByText('«Roma en cinco días» ya no es público.')).toBeInTheDocument();
    expect(getPublicTrips).toHaveBeenCalledTimes(2);
  });

  test('cancelling changes nothing', async () => {
    vi.mocked(getPublicTrips).mockResolvedValue(pageOf([rome]));
    renderAsUser(<PublicTripsPage />, ADMIN, '/public');

    fireEvent.click(await screen.findByRole('button', { name: 'Despublicar Roma en cinco días' }));
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(setPublication).not.toHaveBeenCalled();
    expect(getPublicTrips).toHaveBeenCalledTimes(1);
  });

  test('shows the notice sent by the preview page and lets the user dismiss it', async () => {
    vi.mocked(getPublicTrips).mockResolvedValue(pageOf([oslo]));
    renderRoutes([{ path: '/public', element: <PublicTripsPage /> }], '/public', ADMIN, {
      notice: '«Roma en cinco días» ya no es público.',
    });

    expect(await screen.findByText('«Roma en cinco días» ya no es público.')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /close/i }));

    expect(screen.queryByText('«Roma en cinco días» ya no es público.')).not.toBeInTheDocument();
  });
});

describe('I8 PublicTripsPage', () => {
  test('lists the public trips with destination, dates and author, each linking to its preview', async () => {
    vi.mocked(getPublicTrips).mockResolvedValue(pageOf([rome, oslo]));

    renderPage();

    expect(screen.getByRole('heading', { level: 1, name: 'Viajes públicos' })).toBeInTheDocument();
    expect(await screen.findByRole('link', { name: 'Roma en cinco días' })).toHaveAttribute(
      'href',
      '/public/10',
    );
    expect(screen.getByRole('link', { name: 'Fiordos' })).toHaveAttribute('href', '/public/11');
    expect(screen.getByText(/^Roma · .* · Por Carlos$/)).toBeInTheDocument();
    expect(screen.getByText(/^Oslo · .* · Por Ana$/)).toBeInTheDocument();
    expect(getPublicTrips).toHaveBeenCalledWith(0, 10, '');
  });

  test('searching asks for the first page of the trimmed text and replaces the list', async () => {
    vi.mocked(getPublicTrips).mockResolvedValueOnce(pageOf([rome, oslo]));
    vi.mocked(getPublicTrips).mockResolvedValueOnce(pageOf([oslo]));
    renderPage();
    await screen.findByRole('link', { name: 'Roma en cinco días' });

    search('  oslo ');

    await waitFor(() =>
      expect(screen.queryByRole('link', { name: 'Roma en cinco días' })).not.toBeInTheDocument(),
    );
    expect(screen.getByRole('link', { name: 'Fiordos' })).toBeInTheDocument();
    expect(getPublicTrips).toHaveBeenLastCalledWith(0, 10, 'oslo');
  });

  test('"Más resultados" loads the next page and disappears on the last one', async () => {
    vi.mocked(getPublicTrips).mockResolvedValueOnce(
      pageOf([rome], { page: 0, size: 1, totalElements: 2, last: false }),
    );
    vi.mocked(getPublicTrips).mockResolvedValueOnce(
      pageOf([oslo], { page: 1, size: 1, totalElements: 2, last: true }),
    );
    renderPage();
    await screen.findByRole('link', { name: 'Roma en cinco días' });

    fireEvent.click(screen.getByRole('button', { name: 'Más resultados' }));

    expect(await screen.findByRole('link', { name: 'Fiordos' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Roma en cinco días' })).toBeInTheDocument();
    expect(getPublicTrips).toHaveBeenLastCalledWith(1, 10, '');
    expect(screen.queryByRole('button', { name: 'Más resultados' })).not.toBeInTheDocument();
  });

  test('says there are no public trips yet', async () => {
    vi.mocked(getPublicTrips).mockResolvedValue(pageOf([]));

    renderPage();

    expect(await screen.findByText('Aún no hay viajes públicos.')).toBeInTheDocument();
  });

  test('says so when nothing matches the search', async () => {
    vi.mocked(getPublicTrips).mockResolvedValueOnce(pageOf([rome]));
    vi.mocked(getPublicTrips).mockResolvedValueOnce(pageOf([]));
    renderPage();
    await screen.findByRole('link', { name: 'Roma en cinco días' });

    search('zzz');

    expect(
      await screen.findByText(
        'Ningún viaje público coincide con la búsqueda. Prueba con otro nombre o destino.',
      ),
    ).toBeInTheDocument();
  });

  test('a blank search shows every trip again', async () => {
    vi.mocked(getPublicTrips).mockResolvedValueOnce(pageOf([rome]));
    vi.mocked(getPublicTrips).mockResolvedValueOnce(pageOf([]));
    vi.mocked(getPublicTrips).mockResolvedValueOnce(pageOf([rome]));
    renderPage();
    await screen.findByRole('link', { name: 'Roma en cinco días' });
    search('zzz');
    await screen.findByText(
      'Ningún viaje público coincide con la búsqueda. Prueba con otro nombre o destino.',
    );

    search('   ');

    expect(await screen.findByRole('link', { name: 'Roma en cinco días' })).toBeInTheDocument();
    expect(getPublicTrips).toHaveBeenLastCalledWith(0, 10, '');
  });

  test('a failing request shows the error page of the route', async () => {
    vi.mocked(getPublicTrips).mockRejectedValue(new ApiError(500));
    vi.spyOn(console, 'error').mockImplementation(() => undefined);

    renderRoutes(
      [{ path: '/public', element: <PublicTripsPage />, errorElement: <p>Página de error</p> }],
      '/public',
      null,
    );

    expect(await screen.findByText('Página de error')).toBeInTheDocument();
  });
});
