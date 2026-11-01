import { fireEvent, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import AdminTripsPage from './AdminTripsPage';
import { renderAsUser, renderRoutes } from '../renderHelpers';
import { getTrips } from '../services/adminTripService';
import { ApiError } from '../services/apiError';
import { adminTripOf, ANA, pageOf } from '../testHelpers';
import type { User } from '../types';

vi.mock('../services/adminTripService', () => ({ getTrips: vi.fn() }));

const ADMIN: User = { ...ANA, id: 9, name: 'Administrator', role: 'ADMIN' };
const lisbon = adminTripOf({ id: 3, name: 'Escapada a Lisboa', creatorEmail: 'ana@example.com' });
const oslo = adminTripOf({
  id: 4,
  name: 'Fiordos',
  destination: 'Oslo',
  startDate: '2027-07-10',
  endDate: '2027-07-17',
  creatorEmail: 'bob@example.com',
});

function renderPage() {
  return renderAsUser(<AdminTripsPage />, ADMIN, '/admin/trips');
}

function search(text: string) {
  fireEvent.change(screen.getByLabelText('Buscar por nombre, destino o email del creador'), {
    target: { value: text },
  });
  fireEvent.click(screen.getByRole('button', { name: 'Buscar' }));
}

beforeEach(() => {
  vi.mocked(getTrips).mockReset();
});

describe('B8 AdminTripsPage', () => {
  test('lists the trips with destination, dates, creator email and a link to the detail', async () => {
    vi.mocked(getTrips).mockResolvedValue(pageOf([lisbon, oslo]));

    renderPage();

    expect(await screen.findByRole('heading', { name: 'Escapada a Lisboa' })).toBeInTheDocument();
    expect(screen.getByText('Lisboa')).toBeInTheDocument();
    expect(screen.getByText('1 may 2027 – 5 may 2027')).toBeInTheDocument();
    expect(screen.getByText('Creado por ana@example.com')).toBeInTheDocument();
    expect(screen.getByText('Creado por bob@example.com')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ver viaje Fiordos' })).toHaveAttribute(
      'href',
      '/trips/4',
    );
    expect(getTrips).toHaveBeenCalledWith('', 0);
  });

  test('searching asks for the first page of the trimmed text and replaces the list', async () => {
    vi.mocked(getTrips).mockResolvedValueOnce(pageOf([lisbon, oslo]));
    vi.mocked(getTrips).mockResolvedValueOnce(pageOf([oslo]));
    renderPage();
    await screen.findByText('Escapada a Lisboa');

    search('  oslo ');

    await waitFor(() => expect(screen.queryByText('Escapada a Lisboa')).not.toBeInTheDocument());
    expect(screen.getByRole('heading', { name: 'Fiordos' })).toBeInTheDocument();
    expect(getTrips).toHaveBeenLastCalledWith('oslo', 0);
  });

  test('says so when nothing matches the search', async () => {
    vi.mocked(getTrips).mockResolvedValueOnce(pageOf([lisbon]));
    vi.mocked(getTrips).mockResolvedValueOnce(pageOf([]));
    renderPage();
    await screen.findByText('Escapada a Lisboa');

    search('zzz');

    expect(await screen.findByText('Ningún viaje coincide con la búsqueda.')).toBeInTheDocument();
  });

  test('states it when there are no trips at all', async () => {
    vi.mocked(getTrips).mockResolvedValue(pageOf([]));

    renderPage();

    expect(await screen.findByText('Aún no hay viajes.')).toBeInTheDocument();
  });

  test('"Más resultados" loads the next page and then disappears', async () => {
    vi.mocked(getTrips).mockResolvedValueOnce(pageOf([lisbon], { last: false }));
    vi.mocked(getTrips).mockResolvedValueOnce(pageOf([oslo], { page: 1 }));
    renderPage();
    await screen.findByText('Escapada a Lisboa');

    fireEvent.click(screen.getByRole('button', { name: 'Más resultados' }));

    expect(await screen.findByRole('heading', { name: 'Fiordos' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Escapada a Lisboa' })).toBeInTheDocument();
    expect(getTrips).toHaveBeenLastCalledWith('', 1);
    expect(screen.queryByRole('button', { name: 'Más resultados' })).not.toBeInTheDocument();
  });

  test('hands a loading failure to the route error page', async () => {
    vi.mocked(getTrips).mockRejectedValue(new ApiError(500));

    renderRoutes(
      [{ path: '/', element: <AdminTripsPage />, errorElement: <p>Página de error</p> }],
      '/',
      ADMIN,
    );

    expect(await screen.findByText('Página de error')).toBeInTheDocument();
  });
});
