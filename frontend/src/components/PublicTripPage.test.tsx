import { fireEvent, screen } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { renderRoutes } from '../renderHelpers';
import { ApiError } from '../services/apiError';
import { getPublicActivities, getPublicTrip } from '../services/publicTripService';
import { activityOf, ANA, pageOf, publicTripOf } from '../testHelpers';
import PublicTripPage from './PublicTripPage';

vi.mock('../services/publicTripService', () => ({
  getPublicTrip: vi.fn(),
  getPublicActivities: vi.fn(),
}));

const trip = { ...publicTripOf().trip, description: 'Lo mejor de Roma.' };

const routes = [
  { path: '/public/:tripId', element: <PublicTripPage />, errorElement: <p>Error</p> },
  { path: '/', element: <p>Inicio</p> },
];

beforeEach(() => {
  vi.mocked(getPublicTrip).mockReset().mockResolvedValue(trip);
  vi.mocked(getPublicActivities)
    .mockReset()
    .mockImplementation((_tripId, day) =>
      Promise.resolve(
        pageOf(
          day === '2027-05-11'
            ? [activityOf({ id: 2, name: 'Vaticano', date: day })]
            : [activityOf({ id: 1, name: 'Coliseo', date: day ?? '2027-05-10' })],
        ),
      ),
    );
});

describe('B1 I7 PublicTripPage', () => {
  test('shows the trip data, its author and the first day of the itinerary', async () => {
    renderRoutes(routes, '/public/10', null);

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Roma en cinco días' }),
    ).toBeInTheDocument();
    expect(screen.getByText('Roma')).toBeInTheDocument();
    expect(screen.getByText('10 may 2027 – 14 may 2027')).toBeInTheDocument();
    expect(screen.getByText('1200,00 €')).toBeInTheDocument();
    expect(screen.getByText('Carlos')).toBeInTheDocument();
    expect(screen.getByText('Lo mejor de Roma.')).toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'Coliseo' })).toBeInTheDocument();
    expect(getPublicTrip).toHaveBeenCalledWith(10);
    expect(getPublicActivities).toHaveBeenCalledWith(10, '2027-05-10', 0);
  });

  test('browses the days of the itinerary', async () => {
    renderRoutes(routes, '/public/10', null);
    await screen.findByRole('heading', { name: 'Coliseo' });

    fireEvent.click(screen.getByRole('tab', { name: /Día 2/ }));

    expect(await screen.findByRole('heading', { name: 'Vaticano' })).toBeInTheDocument();
    expect(getPublicActivities).toHaveBeenCalledWith(10, '2027-05-11', 0);
  });

  test('is read-only for a logged-in user too: no edit, delete, new, publish or conflicts', async () => {
    renderRoutes(routes, '/public/10', ANA);
    await screen.findByRole('heading', { name: 'Coliseo' });

    expect(screen.queryByRole('link', { name: 'Nueva actividad' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Editar Coliseo' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Eliminar Coliseo' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Despublicar' })).not.toBeInTheDocument();
    expect(screen.queryByText('Presupuesto restante')).not.toBeInTheDocument();
    expect(screen.queryByRole('region', { name: /conflictos/i })).not.toBeInTheDocument();
  });

  test('has a link back to the home page', async () => {
    renderRoutes(routes, '/public/10', null);
    await screen.findByRole('heading', { name: 'Roma en cinco días' });

    fireEvent.click(screen.getByRole('link', { name: 'Volver al inicio' }));

    expect(await screen.findByText('Inicio')).toBeInTheDocument();
  });

  test('omits the budget of a trip without one', async () => {
    vi.mocked(getPublicTrip).mockResolvedValue({ ...trip, budget: null, description: null });
    renderRoutes(routes, '/public/10', null);

    await screen.findByRole('heading', { name: 'Roma en cinco días' });

    expect(screen.queryByText('Presupuesto')).not.toBeInTheDocument();
  });

  test('a trip that is not published shows the route error page', async () => {
    vi.mocked(getPublicTrip).mockRejectedValue(new ApiError(404));

    renderRoutes(routes, '/public/10', null);

    expect(await screen.findByText('Error')).toBeInTheDocument();
  });

  test('an id that is not a number shows the route error page without any request', async () => {
    renderRoutes(routes, '/public/abc', null);

    expect(await screen.findByText('Error')).toBeInTheDocument();
    expect(getPublicTrip).not.toHaveBeenCalled();
  });
});
