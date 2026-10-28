import { fireEvent, screen } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { renderRoutes } from '../renderHelpers';
import { ApiError } from '../services/apiError';
import { getExampleActivities, getExampleTrip } from '../services/exampleService';
import { activityOf, ANA, pageOf, tripOf } from '../testHelpers';
import ExampleTripPage from './ExampleTripPage';

vi.mock('../services/exampleService', () => ({
  getExampleTrip: vi.fn(),
  getExampleActivities: vi.fn(),
}));

const trip = tripOf({
  id: 10,
  creatorId: ANA.id,
  name: 'Roma en cinco días',
  destination: 'Roma',
  startDate: '2027-05-10',
  endDate: '2027-05-14',
  budget: 1200,
  description: 'Lo mejor de Roma.',
});

const routes = [
  { path: '/examples/:tripId', element: <ExampleTripPage />, errorElement: <p>Error</p> },
  { path: '/', element: <p>Inicio</p> },
];

beforeEach(() => {
  vi.mocked(getExampleTrip).mockReset().mockResolvedValue(trip);
  vi.mocked(getExampleActivities)
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

describe('B1 ExampleTripPage', () => {
  test('shows the cover, the trip data and the first day of the itinerary', async () => {
    renderRoutes(routes, '/examples/10', null);

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Roma en cinco días' }),
    ).toBeInTheDocument();
    expect(screen.getByText('Roma')).toBeInTheDocument();
    expect(screen.getByText('10 may 2027 – 14 may 2027')).toBeInTheDocument();
    expect(screen.getByText('1200,00 €')).toBeInTheDocument();
    expect(screen.getByText('Lo mejor de Roma.')).toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'Coliseo' })).toBeInTheDocument();
    expect(getExampleTrip).toHaveBeenCalledWith(10);
    expect(getExampleActivities).toHaveBeenCalledWith(10, '2027-05-10', 0);
  });

  test('browses the days of the itinerary', async () => {
    renderRoutes(routes, '/examples/10', null);
    await screen.findByRole('heading', { name: 'Coliseo' });

    fireEvent.click(screen.getByRole('tab', { name: /Día 2/ }));

    expect(await screen.findByRole('heading', { name: 'Vaticano' })).toBeInTheDocument();
    expect(getExampleActivities).toHaveBeenCalledWith(10, '2027-05-11', 0);
  });

  test('is read-only, even for the user that created the trip: no edit, delete, new or conflicts', async () => {
    renderRoutes(routes, '/examples/10', ANA);
    await screen.findByRole('heading', { name: 'Coliseo' });

    expect(screen.queryByRole('link', { name: 'Nueva actividad' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Editar Coliseo' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Eliminar Coliseo' })).not.toBeInTheDocument();
    expect(screen.queryByText('Presupuesto restante')).not.toBeInTheDocument();
    expect(screen.queryByRole('region', { name: /conflictos/i })).not.toBeInTheDocument();
  });

  test('has a link back to the home page', async () => {
    renderRoutes(routes, '/examples/10', null);
    await screen.findByRole('heading', { name: 'Roma en cinco días' });

    fireEvent.click(screen.getByRole('link', { name: 'Volver al inicio' }));

    expect(await screen.findByText('Inicio')).toBeInTheDocument();
  });

  test('omits the budget of a trip without one', async () => {
    vi.mocked(getExampleTrip).mockResolvedValue({ ...trip, budget: null, description: null });
    renderRoutes(routes, '/examples/10', null);

    await screen.findByRole('heading', { name: 'Roma en cinco días' });

    expect(screen.queryByText('Presupuesto')).not.toBeInTheDocument();
  });

  test('a trip that is not an example shows the route error page', async () => {
    vi.mocked(getExampleTrip).mockRejectedValue(new ApiError(404));

    renderRoutes(routes, '/examples/10', null);

    expect(await screen.findByText('Error')).toBeInTheDocument();
  });

  test('an id that is not a number shows the route error page without any request', async () => {
    renderRoutes(routes, '/examples/abc', null);

    expect(await screen.findByText('Error')).toBeInTheDocument();
    expect(getExampleTrip).not.toHaveBeenCalled();
  });
});
