import { fireEvent, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { renderRoutes } from '../renderHelpers';
import { ApiError } from '../services/apiError';
import { deleteTrip, getTrip } from '../services/tripService';
import { ANA, tripOf } from '../testHelpers';
import TripDetailPage from './TripDetailPage';

vi.mock('../services/tripService', () => ({ deleteTrip: vi.fn(), getTrip: vi.fn() }));

const routes = [
  { path: '/trips', element: <p>Lista de viajes</p> },
  { path: '/trips/:tripId', element: <TripDetailPage />, errorElement: <p>Página de error</p> },
];

const trip = tripOf({
  id: 5,
  creatorId: ANA.id,
  name: 'Verano en Roma',
  destination: 'Roma',
  startDate: '2027-07-10',
  endDate: '2027-07-17',
  budget: 1200.5,
  description: 'Ruinas y pasta.',
  coverImageId: 'abc-123',
});

beforeEach(() => {
  vi.mocked(deleteTrip).mockReset();
  vi.mocked(getTrip).mockReset();
});

describe('B4 TripDetailPage', () => {
  test('shows a loading text and then the data of the trip with its cover', async () => {
    vi.mocked(getTrip).mockResolvedValue(trip);
    renderRoutes(routes, '/trips/5');

    expect(screen.getByRole('status')).toHaveTextContent('Cargando viaje…');
    expect(await screen.findByRole('heading', { name: 'Verano en Roma' })).toBeInTheDocument();
    expect(screen.getByText('Roma')).toBeInTheDocument();
    expect(screen.getByText('10 jul 2027 – 17 jul 2027')).toBeInTheDocument();
    expect(screen.getByText(/1200,50|1\.200,50/)).toBeInTheDocument();
    expect(screen.getByText('Ruinas y pasta.')).toBeInTheDocument();
    expect(screen.getByAltText('Portada de Verano en Roma')).toHaveAttribute(
      'src',
      '/api/v1/images/abc-123',
    );
    expect(getTrip).toHaveBeenCalledWith(5);
  });

  test('leaves out the budget and the description when the trip has none', async () => {
    vi.mocked(getTrip).mockResolvedValue({
      ...trip,
      budget: null,
      description: null,
      coverImageId: null,
    });
    renderRoutes(routes, '/trips/5');

    await screen.findByRole('heading', { name: 'Verano en Roma' });

    expect(screen.queryByText('Presupuesto')).not.toBeInTheDocument();
    expect(screen.queryByText('Ruinas y pasta.')).not.toBeInTheDocument();
    expect(screen.queryByAltText(/Portada de/)).not.toBeInTheDocument();
  });

  test('offers "Editar" and "Eliminar" to the creator', async () => {
    vi.mocked(getTrip).mockResolvedValue(trip);
    renderRoutes(routes, '/trips/5');

    expect(await screen.findByRole('link', { name: 'Editar' })).toHaveAttribute(
      'href',
      '/trips/5/edit',
    );
    expect(screen.getByRole('button', { name: 'Eliminar' })).toBeInTheDocument();
  });

  test('hides both actions from anyone else', async () => {
    vi.mocked(getTrip).mockResolvedValue({ ...trip, creatorId: ANA.id + 1 });
    renderRoutes(routes, '/trips/5');

    await screen.findByRole('heading', { name: 'Verano en Roma' });

    expect(screen.queryByRole('link', { name: 'Editar' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Eliminar' })).not.toBeInTheDocument();
  });

  test('deleting asks first, keeps the trip when cancelled and goes back to the list when confirmed', async () => {
    vi.mocked(getTrip).mockResolvedValue(trip);
    vi.mocked(deleteTrip).mockResolvedValue(undefined);
    const router = renderRoutes(routes, '/trips/5');
    fireEvent.click(await screen.findByRole('button', { name: 'Eliminar' }));
    expect(screen.getByRole('dialog', { name: 'Eliminar viaje' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));
    expect(deleteTrip).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', { name: 'Eliminar' }));
    fireEvent.click(screen.getByRole('button', { name: 'Eliminar viaje' }));

    await waitFor(() => expect(router.state.location.pathname).toBe('/trips'));
    expect(deleteTrip).toHaveBeenCalledWith(5);
    expect(await screen.findByText('Lista de viajes')).toBeInTheDocument();
  });

  test('a trip that cannot be loaded goes to the route error page', async () => {
    vi.mocked(getTrip).mockRejectedValue(new ApiError(403));
    renderRoutes(routes, '/trips/5');

    expect(await screen.findByText('Página de error')).toBeInTheDocument();
  });

  test('an id that is not a number is a missing trip and calls nothing', async () => {
    renderRoutes(routes, '/trips/abc');

    expect(await screen.findByText('Página de error')).toBeInTheDocument();
    expect(getTrip).not.toHaveBeenCalled();
  });

  test('links back to the list', async () => {
    vi.mocked(getTrip).mockResolvedValue(trip);
    renderRoutes(routes, '/trips/5');

    expect(await screen.findByRole('link', { name: 'Volver a Mis viajes' })).toHaveAttribute(
      'href',
      '/trips',
    );
  });
});
