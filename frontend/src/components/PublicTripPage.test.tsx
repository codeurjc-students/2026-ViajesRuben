import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { renderRoutes } from '../renderHelpers';
import { ApiError } from '../services/apiError';
import { copyPublicTrip, getPublicActivities, getPublicTrip } from '../services/publicTripService';
import { setPublication } from '../services/tripService';
import { activityOf, ANA, pageOf, publicTripOf, tripOf } from '../testHelpers';
import PublicTripPage from './PublicTripPage';

vi.mock('../services/publicTripService', () => ({
  getPublicTrip: vi.fn(),
  getPublicActivities: vi.fn(),
  copyPublicTrip: vi.fn(),
}));
vi.mock('../services/tripService', () => ({ setPublication: vi.fn() }));

const trip = { ...publicTripOf().trip, description: 'Lo mejor de Roma.' };

const routes = [
  { path: '/public/:tripId', element: <PublicTripPage />, errorElement: <p>Error</p> },
  { path: '/', element: <p>Inicio</p> },
  { path: '/login', element: <p>Iniciar sesión</p> },
  { path: '/trips/:tripId', element: <p>Detalle del viaje</p> },
];

beforeEach(() => {
  vi.mocked(setPublication).mockReset();
  vi.mocked(copyPublicTrip).mockReset();
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

  test('I4 has no change log and says nothing about who added each activity', async () => {
    vi.mocked(getPublicActivities)
      .mockReset()
      .mockResolvedValue(
        pageOf([activityOf({ id: 1, name: 'Coliseo', date: '2027-05-10', authorName: null })]),
      );
    renderRoutes(routes, '/public/10', ANA);

    expect(await screen.findByRole('heading', { name: 'Coliseo' })).toBeInTheDocument();

    expect(screen.queryByRole('heading', { name: 'Historial de cambios' })).not.toBeInTheDocument();
    expect(screen.queryByText(/Añadida por/)).not.toBeInTheDocument();
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

describe('I10 PublicTripPage moderation', () => {
  const admin = { ...ANA, id: 99, name: 'Admin', role: 'ADMIN' as const };
  const moderationRoutes = [...routes, { path: '/public', element: <p>Lista pública</p> }];

  test.each([
    ['a visitor', null],
    ['a regular user', ANA],
  ])('%s does not see "Despublicar"', async (_who, user) => {
    renderRoutes(routes, '/public/10', user);
    await screen.findByRole('heading', { name: 'Roma en cinco días' });

    expect(screen.queryByRole('button', { name: 'Despublicar' })).not.toBeInTheDocument();
  });

  test('the administrator unpublishes and goes back to the list with a notice', async () => {
    vi.mocked(setPublication).mockResolvedValue(tripOf({ id: 10, published: false }));
    const router = renderRoutes(moderationRoutes, '/public/10', admin);

    fireEvent.click(await screen.findByRole('button', { name: 'Despublicar' }));
    const dialog = await screen.findByRole('dialog', { name: 'Despublicar itinerario' });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Despublicar' }));

    expect(await screen.findByText('Lista pública')).toBeInTheDocument();
    expect(setPublication).toHaveBeenCalledWith(10, false);
    expect(router.state.location.pathname).toBe('/public');
    expect(router.state.location.state).toEqual({
      notice: '«Roma en cinco días» ya no es público.',
    });
  });

  test('cancelling stays on the preview without unpublishing', async () => {
    const router = renderRoutes(moderationRoutes, '/public/10', admin);

    fireEvent.click(await screen.findByRole('button', { name: 'Despublicar' }));
    fireEvent.click(await screen.findByRole('button', { name: 'Cancelar' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(setPublication).not.toHaveBeenCalled();
    expect(router.state.location.pathname).toBe('/public/10');
  });
});

describe('I9 PublicTripPage copy', () => {
  async function openCopyDialog() {
    fireEvent.click(await screen.findByRole('button', { name: 'Copiar en tu cuenta' }));
    return screen.findByRole('dialog', { name: 'Copiar en tu cuenta' });
  }

  test('offers the copy button to visitors and to logged-in users', async () => {
    renderRoutes(routes, '/public/10', null);

    expect(await screen.findByRole('button', { name: 'Copiar en tu cuenta' })).toBeEnabled();
  });

  test('a visitor is sent to the login page, remembering this page to come back to it', async () => {
    const router = renderRoutes(routes, '/public/10', null);

    fireEvent.click(await screen.findByRole('button', { name: 'Copiar en tu cuenta' }));

    expect(await screen.findByText('Iniciar sesión')).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/login');
    expect(router.state.location.state).toMatchObject({ from: { pathname: '/public/10' } });
    expect(copyPublicTrip).not.toHaveBeenCalled();
  });

  test('a logged-in user gets the modal, not a redirect, and nothing is copied yet', async () => {
    const router = renderRoutes(routes, '/public/10', ANA);

    const dialog = await openCopyDialog();

    expect(dialog).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/public/10');
    expect(copyPublicTrip).not.toHaveBeenCalled();
  });

  test('the modal is not shown to a visitor', async () => {
    renderRoutes(routes, '/public/10', null);
    await screen.findByRole('heading', { name: 'Roma en cinco días' });

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  test('copies with the chosen date and lands on the new trip with a success notice', async () => {
    vi.mocked(copyPublicTrip).mockResolvedValue(tripOf({ id: 20, copiedFromId: 10 }));
    const router = renderRoutes(routes, '/public/10', ANA);
    await openCopyDialog();
    fireEvent.change(screen.getByLabelText('Nueva fecha de inicio (opcional)'), {
      target: { value: '2027-09-01' },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Copiar viaje' }));

    expect(await screen.findByText('Detalle del viaje')).toBeInTheDocument();
    expect(copyPublicTrip).toHaveBeenCalledWith(10, '2027-09-01');
    expect(router.state.location.pathname).toBe('/trips/20');
    expect(router.state.location.state).toEqual({
      notice: 'Itinerario copiado en tus viajes. Ya puedes editarlo.',
    });
  });

  test('copies with the original dates when no date is chosen', async () => {
    vi.mocked(copyPublicTrip).mockResolvedValue(tripOf({ id: 21 }));
    const router = renderRoutes(routes, '/public/10', ANA);
    await openCopyDialog();

    fireEvent.click(screen.getByRole('button', { name: 'Copiar viaje' }));

    await screen.findByText('Detalle del viaje');
    expect(copyPublicTrip).toHaveBeenCalledWith(10, undefined);
    expect(router.state.location.pathname).toBe('/trips/21');
  });

  test('a failure is shown inside the modal and the user stays on the page', async () => {
    vi.mocked(copyPublicTrip).mockRejectedValue(new ApiError(500));
    const router = renderRoutes(routes, '/public/10', ANA);
    await openCopyDialog();

    fireEvent.click(screen.getByRole('button', { name: 'Copiar viaje' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudo copiar el itinerario. Inténtalo de nuevo.',
    );
    expect(router.state.location.pathname).toBe('/public/10');
    expect(screen.getByRole('button', { name: 'Copiar viaje' })).toBeEnabled();
  });

  test('cancelling closes the modal without copying', async () => {
    renderRoutes(routes, '/public/10', ANA);
    await openCopyDialog();

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(copyPublicTrip).not.toHaveBeenCalled();
  });
});
