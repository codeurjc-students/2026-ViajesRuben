import { fireEvent, screen } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { renderRoutes } from '../renderHelpers';
import {
  applyActivityImage,
  createActivity,
  getActivity,
  updateActivity,
} from '../services/activityService';
import { ApiError, NetworkError } from '../services/apiError';
import { getTrip } from '../services/tripService';
import { activityOf, tripOf } from '../testHelpers';
import ActivityFormPage from './ActivityFormPage';

vi.mock('../services/activityService', () => ({
  applyActivityImage: vi.fn(),
  createActivity: vi.fn(),
  getActivity: vi.fn(),
  updateActivity: vi.fn(),
}));
vi.mock('../services/tripService', () => ({ getTrip: vi.fn() }));

const trip = tripOf({ id: 3, name: 'Escapada', startDate: '2027-05-01', endDate: '2027-05-05' });

const routes = [
  {
    path: '/trips/:tripId',
    element: <p>Detalle del viaje</p>,
  },
  {
    path: '/trips/:tripId/activities/new',
    element: <ActivityFormPage />,
    errorElement: <p>Página de error</p>,
  },
  {
    path: '/trips/:tripId/activities/:activityId/edit',
    element: <ActivityFormPage />,
    errorElement: <p>Página de error</p>,
  },
];

function type(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } });
}

beforeEach(() => {
  vi.mocked(applyActivityImage).mockReset().mockResolvedValue(undefined);
  vi.mocked(createActivity).mockReset();
  vi.mocked(getActivity).mockReset();
  vi.mocked(updateActivity).mockReset();
  vi.mocked(getTrip).mockReset().mockResolvedValue(trip);
  URL.createObjectURL = vi.fn(() => 'blob:preview');
  URL.revokeObjectURL = vi.fn();
});

describe('B5 creating an activity', () => {
  test('loads the trip and shows the empty form on the first day', async () => {
    renderRoutes(routes, '/trips/3/activities/new');

    expect(screen.getByRole('status')).toHaveTextContent('Cargando actividad…');
    expect(await screen.findByLabelText('Nombre de la actividad')).toHaveValue('');
    expect(screen.getByRole('heading', { name: 'Nueva actividad' })).toBeInTheDocument();
    expect(screen.getByText('Viaje: Escapada')).toBeInTheDocument();
    expect(screen.getByLabelText('Día')).toHaveValue('2027-05-01');
    expect(getActivity).not.toHaveBeenCalled();
  });

  test('starts on the day of the URL when it is a day of the trip', async () => {
    renderRoutes(routes, '/trips/3/activities/new?day=2027-05-04');

    expect(await screen.findByLabelText('Día')).toHaveValue('2027-05-04');
  });

  test('ignores a day of the URL that is outside the trip', async () => {
    renderRoutes(routes, '/trips/3/activities/new?day=2030-01-01');

    expect(await screen.findByLabelText('Día')).toHaveValue('2027-05-01');
  });

  test('creates the activity, applies the image and goes back to that day of the trip', async () => {
    vi.mocked(createActivity).mockResolvedValue(
      activityOf({ id: 9, tripId: 3, date: '2027-05-03' }),
    );
    const router = renderRoutes(routes, '/trips/3/activities/new?day=2027-05-03');
    await screen.findByLabelText('Día');
    type('Nombre de la actividad', 'Museo');
    const file = new File([new Uint8Array(4)], 'a.png', { type: 'image/png' });
    fireEvent.change(screen.getByLabelText('Imagen'), { target: { files: [file] } });

    fireEvent.click(screen.getByRole('button', { name: 'Crear actividad' }));

    expect(await screen.findByText('Detalle del viaje')).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/trips/3');
    expect(router.state.location.search).toBe('?day=2027-05-03');
    expect(createActivity).toHaveBeenCalledWith(
      3,
      expect.objectContaining({ name: 'Museo', date: '2027-05-03' }),
    );
    expect(applyActivityImage).toHaveBeenCalledWith(3, 9, { kind: 'replace', file });
  });

  test('does not call the server while the form has errors', async () => {
    renderRoutes(routes, '/trips/3/activities/new');
    await screen.findByLabelText('Nombre de la actividad');

    fireEvent.click(screen.getByRole('button', { name: 'Crear actividad' }));

    expect(screen.getByText('Escribe el nombre de la actividad.')).toBeInTheDocument();
    expect(createActivity).not.toHaveBeenCalled();
  });

  test.each([
    [new ApiError(400), 'Revisa los datos del formulario.'],
    [new ApiError(401), 'Tu sesión ha caducado. Inicia sesión de nuevo.'],
    [new ApiError(403), 'No puedes modificar este viaje.'],
    [new ApiError(404), 'Esta actividad ya no existe.'],
    [new NetworkError(), 'No se pudo conectar con el servidor. Inténtalo de nuevo.'],
    [new ApiError(500), 'Algo ha fallado en el servidor. Inténtalo de nuevo más tarde.'],
  ])('shows the failure inline and stays on the form (%#)', async (failure, message) => {
    vi.mocked(createActivity).mockRejectedValue(failure);
    const router = renderRoutes(routes, '/trips/3/activities/new');
    await screen.findByLabelText('Día');
    type('Nombre de la actividad', 'Museo');

    fireEvent.click(screen.getByRole('button', { name: 'Crear actividad' }));

    expect(await screen.findByText(message)).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/trips/3/activities/new');
    expect(screen.getByRole('button', { name: 'Crear actividad' })).toBeEnabled();
    expect(applyActivityImage).not.toHaveBeenCalled();
  });

  test('disables the buttons while the activity is being saved', async () => {
    let finish: (activity: ReturnType<typeof activityOf>) => void = () => {};
    vi.mocked(createActivity).mockReturnValue(
      new Promise((resolve) => {
        finish = resolve;
      }),
    );
    renderRoutes(routes, '/trips/3/activities/new');
    await screen.findByLabelText('Día');
    type('Nombre de la actividad', 'Museo');

    fireEvent.click(screen.getByRole('button', { name: 'Crear actividad' }));

    expect(await screen.findByRole('button', { name: 'Guardando…' })).toBeDisabled();
    finish(activityOf({ id: 9 }));
    expect(await screen.findByText('Detalle del viaje')).toBeInTheDocument();
  });

  test('when only the image fails the activity is kept and its edit page explains it', async () => {
    vi.mocked(createActivity).mockResolvedValue(activityOf({ id: 9, tripId: 3 }));
    vi.mocked(applyActivityImage).mockRejectedValue(new ApiError(413));
    vi.mocked(getActivity).mockResolvedValue(activityOf({ id: 9, tripId: 3 }));
    const router = renderRoutes(routes, '/trips/3/activities/new');
    await screen.findByLabelText('Día');
    type('Nombre de la actividad', 'Museo');

    fireEvent.click(screen.getByRole('button', { name: 'Crear actividad' }));

    expect(
      await screen.findByText('La actividad se ha guardado, pero no se pudo actualizar la imagen', {
        exact: false,
      }),
    ).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/trips/3/activities/9/edit');
    expect(screen.getByRole('heading', { name: 'Editar actividad' })).toBeInTheDocument();
  });

  test('cancelling goes back to the trip on the day the form started on', async () => {
    const router = renderRoutes(routes, '/trips/3/activities/new?day=2027-05-04');
    await screen.findByLabelText('Día');

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(await screen.findByText('Detalle del viaje')).toBeInTheDocument();
    expect(router.state.location.search).toBe('?day=2027-05-04');
    expect(createActivity).not.toHaveBeenCalled();
  });

  test('a trip that cannot be loaded goes to the route error page', async () => {
    vi.mocked(getTrip).mockRejectedValue(new ApiError(403));
    renderRoutes(routes, '/trips/3/activities/new');

    expect(await screen.findByText('Página de error')).toBeInTheDocument();
  });

  test('a trip id that is not a number is a missing trip and calls nothing', async () => {
    renderRoutes(routes, '/trips/abc/activities/new');

    expect(await screen.findByText('Página de error')).toBeInTheDocument();
    expect(getTrip).not.toHaveBeenCalled();
  });
});

describe('B5 editing an activity', () => {
  const activity = activityOf({
    id: 7,
    tripId: 3,
    name: 'Museo',
    date: '2027-05-03',
    startTime: '10:00:00',
    imageId: 'img-1',
  });

  test('loads the trip and the activity and fills the form', async () => {
    vi.mocked(getActivity).mockResolvedValue(activity);
    renderRoutes(routes, '/trips/3/activities/7/edit');

    expect(await screen.findByLabelText('Nombre de la actividad')).toHaveValue('Museo');
    expect(screen.getByRole('heading', { name: 'Editar actividad' })).toBeInTheDocument();
    expect(screen.getByLabelText('Hora de inicio')).toHaveValue('10:00');
    expect(getActivity).toHaveBeenCalledWith(3, 7);
  });

  test('saves the changes and goes back to the day of the activity', async () => {
    vi.mocked(getActivity).mockResolvedValue(activity);
    vi.mocked(updateActivity).mockResolvedValue({ ...activity, date: '2027-05-04' });
    const router = renderRoutes(routes, '/trips/3/activities/7/edit');
    await screen.findByLabelText('Nombre de la actividad');
    type('Nombre de la actividad', 'Museo del azulejo');
    type('Día', '2027-05-04');

    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByText('Detalle del viaje')).toBeInTheDocument();
    expect(router.state.location.search).toBe('?day=2027-05-04');
    expect(updateActivity).toHaveBeenCalledWith(
      3,
      7,
      expect.objectContaining({ name: 'Museo del azulejo', date: '2027-05-04' }),
    );
    expect(applyActivityImage).toHaveBeenCalledWith(3, 7, { kind: 'keep' });
  });

  test('cancelling goes back to the day of the activity', async () => {
    vi.mocked(getActivity).mockResolvedValue(activity);
    const router = renderRoutes(routes, '/trips/3/activities/7/edit');
    await screen.findByLabelText('Nombre de la actividad');

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(await screen.findByText('Detalle del viaje')).toBeInTheDocument();
    expect(router.state.location.search).toBe('?day=2027-05-03');
    expect(updateActivity).not.toHaveBeenCalled();
  });

  test.each([403, 404])(
    'an activity that cannot be loaded (%i) goes to the route error page',
    async (status) => {
      vi.mocked(getActivity).mockRejectedValue(new ApiError(status));
      renderRoutes(routes, '/trips/3/activities/7/edit');

      expect(await screen.findByText('Página de error')).toBeInTheDocument();
    },
  );

  test('an activity id that is not a number calls nothing', async () => {
    renderRoutes(routes, '/trips/3/activities/abc/edit');

    expect(await screen.findByText('Página de error')).toBeInTheDocument();
    expect(getActivity).not.toHaveBeenCalled();
  });

  test('a save that fails because the activity is gone says so', async () => {
    vi.mocked(getActivity).mockResolvedValue(activity);
    vi.mocked(updateActivity).mockRejectedValue(new ApiError(404));
    renderRoutes(routes, '/trips/3/activities/7/edit');
    await screen.findByLabelText('Nombre de la actividad');

    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByText('Esta actividad ya no existe.')).toBeInTheDocument();
  });
});
