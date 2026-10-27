import { fireEvent, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { renderRoutes } from '../renderHelpers';
import { deleteActivity, getActivities } from '../services/activityService';
import { ApiError } from '../services/apiError';
import { getConflicts } from '../services/conflictService';
import { deleteTrip, getTrip } from '../services/tripService';
import { activityOf, ANA, conflictsOf, pageOf, tripOf } from '../testHelpers';
import TripDetailPage from './TripDetailPage';

vi.mock('../services/tripService', () => ({ deleteTrip: vi.fn(), getTrip: vi.fn() }));
vi.mock('../services/activityService', () => ({
  getActivities: vi.fn(),
  deleteActivity: vi.fn(),
}));
vi.mock('../services/conflictService', () => ({ getConflicts: vi.fn() }));

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
  vi.mocked(getActivities).mockReset().mockResolvedValue(pageOf([]));
  vi.mocked(deleteActivity).mockReset();
  vi.mocked(getConflicts).mockReset().mockResolvedValue(conflictsOf());
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

  test('B5 shows the itinerary below the summary, starting on the first day', async () => {
    vi.mocked(getTrip).mockResolvedValue(trip);
    renderRoutes(routes, '/trips/5');

    expect(await screen.findByRole('heading', { name: 'Itinerario' })).toBeInTheDocument();
    expect(screen.getAllByRole('tab')).toHaveLength(8);
    expect(
      await screen.findByText('No hay actividades este día', { exact: false }),
    ).toBeInTheDocument();
    expect(getActivities).toHaveBeenCalledWith(5, '2027-07-10', 0);
    expect(screen.getByRole('link', { name: 'Nueva actividad' })).toHaveAttribute(
      'href',
      '/trips/5/activities/new?day=2027-07-10',
    );
  });

  test('B5 deleting the trip warns that its activities go with it', async () => {
    vi.mocked(getTrip).mockResolvedValue(trip);
    renderRoutes(routes, '/trips/5');
    fireEvent.click(await screen.findByRole('button', { name: 'Eliminar' }));

    expect(screen.getByRole('dialog', { name: 'Eliminar viaje' })).toHaveTextContent(
      'con su portada y sus actividades',
    );
  });

  test('B6 shows no warning when the trip has no conflicts', async () => {
    vi.mocked(getTrip).mockResolvedValue(trip);
    renderRoutes(routes, '/trips/5');

    await screen.findByRole('heading', { name: 'Itinerario' });

    expect(getConflicts).toHaveBeenCalledWith(5);
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  test('B6 shows the warning above the itinerary, with an edit link for the creator', async () => {
    vi.mocked(getTrip).mockResolvedValue(trip);
    vi.mocked(getConflicts).mockResolvedValue(
      conflictsOf({
        outsideDates: [{ activityId: 8, name: 'Museo', date: '2027-07-20' }],
        budget: { totalCost: 2000, budget: 1200.5 },
      }),
    );
    renderRoutes(routes, '/trips/5');

    const alert = await screen.findByRole('alert');

    expect(alert).toHaveTextContent('Conflictos en el itinerario');
    expect(alert).toHaveTextContent('«Museo» está fuera de las fechas del viaje');
    expect(alert).toHaveTextContent('supera el presupuesto');
    expect(screen.getByRole('link', { name: 'Editar «Museo»' })).toHaveAttribute(
      'href',
      '/trips/5/activities/8/edit',
    );
    const itinerary = screen.getByRole('heading', { name: 'Itinerario' });
    expect(
      alert.compareDocumentPosition(itinerary) & Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
  });

  test('B6 gives no edit link to someone who is not the creator', async () => {
    vi.mocked(getTrip).mockResolvedValue({ ...trip, creatorId: ANA.id + 1 });
    vi.mocked(getConflicts).mockResolvedValue(
      conflictsOf({ outsideDates: [{ activityId: 8, name: 'Museo', date: '2027-07-20' }] }),
    );
    renderRoutes(routes, '/trips/5');

    await screen.findByRole('alert');

    expect(screen.queryByRole('link', { name: 'Editar «Museo»' })).not.toBeInTheDocument();
  });

  test('B6 a failure loading the conflicts shows no warning and keeps the page', async () => {
    vi.mocked(getTrip).mockResolvedValue(trip);
    vi.mocked(getConflicts).mockRejectedValue(new ApiError(500));
    renderRoutes(routes, '/trips/5');

    expect(await screen.findByRole('heading', { name: 'Itinerario' })).toBeInTheDocument();
    await waitFor(() => expect(getConflicts).toHaveBeenCalled());
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  test('B6 deleting an activity asks for the conflicts again and the warning goes away', async () => {
    vi.mocked(getTrip).mockResolvedValue(trip);
    vi.mocked(getActivities).mockResolvedValue(
      pageOf([activityOf({ id: 3, tripId: 5, name: 'Museo', date: '2027-07-10' })]),
    );
    vi.mocked(deleteActivity).mockResolvedValue(undefined);
    vi.mocked(getConflicts)
      .mockResolvedValueOnce(conflictsOf({ budget: { totalCost: 2000, budget: 1200.5 } }))
      .mockResolvedValue(conflictsOf());
    renderRoutes(routes, '/trips/5');
    await screen.findByRole('alert');

    fireEvent.click(await screen.findByRole('button', { name: 'Eliminar Museo' }));
    fireEvent.click(screen.getByRole('button', { name: 'Eliminar actividad' }));

    await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
    expect(deleteActivity).toHaveBeenCalledWith(5, 3);
    expect(getConflicts).toHaveBeenCalledTimes(2);
  });
});
