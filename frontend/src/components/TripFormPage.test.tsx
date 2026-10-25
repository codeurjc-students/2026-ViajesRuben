import { fireEvent, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { renderRoutes } from '../renderHelpers';
import { ApiError, NetworkError } from '../services/apiError';
import { applyCover, createTrip, getTrip, updateTrip } from '../services/tripService';
import { tripOf } from '../testHelpers';
import TripFormPage from './TripFormPage';

vi.mock('../services/tripService', () => ({
  applyCover: vi.fn(),
  createTrip: vi.fn(),
  getTrip: vi.fn(),
  updateTrip: vi.fn(),
}));

const routes = [
  { path: '/trips', element: <p>Mis viajes</p> },
  { path: '/trips/new', element: <TripFormPage /> },
  { path: '/trips/:tripId', element: <p>Detalle del viaje</p> },
  { path: '/trips/:tripId/edit', element: <TripFormPage />, errorElement: <p>Página de error</p> },
];

function type(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } });
}

function fillRequired() {
  type('Nombre del viaje', 'Escapada');
  type('Destino', 'Lisboa');
  type('Fecha de inicio', '2027-05-01');
  type('Fecha de fin', '2027-05-05');
}

beforeEach(() => {
  vi.mocked(applyCover).mockReset().mockResolvedValue(undefined);
  vi.mocked(createTrip).mockReset();
  vi.mocked(getTrip).mockReset();
  vi.mocked(updateTrip).mockReset();
  URL.createObjectURL = vi.fn(() => 'blob:preview');
  URL.revokeObjectURL = vi.fn();
});

describe('B4 creating a trip', () => {
  test('shows the empty form under "Nuevo viaje"', () => {
    renderRoutes(routes, '/trips/new');

    expect(screen.getByRole('heading', { name: 'Nuevo viaje' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Crear viaje' })).toBeInTheDocument();
    expect(getTrip).not.toHaveBeenCalled();
  });

  test('creates the trip, applies the cover and opens the trip', async () => {
    vi.mocked(createTrip).mockResolvedValue(tripOf({ id: 9 }));
    const router = renderRoutes(routes, '/trips/new');
    fillRequired();
    const file = new File([new Uint8Array(4)], 'a.png', { type: 'image/png' });
    fireEvent.change(screen.getByLabelText('Portada'), { target: { files: [file] } });

    fireEvent.click(screen.getByRole('button', { name: 'Crear viaje' }));

    expect(await screen.findByText('Detalle del viaje')).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/trips/9');
    expect(createTrip).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'Escapada', destination: 'Lisboa', budget: null }),
    );
    expect(applyCover).toHaveBeenCalledWith(9, { kind: 'replace', file });
  });

  test('does not call the server while the form has errors', () => {
    renderRoutes(routes, '/trips/new');

    fireEvent.click(screen.getByRole('button', { name: 'Crear viaje' }));

    expect(screen.getByText('Escribe el nombre del viaje.')).toBeInTheDocument();
    expect(createTrip).not.toHaveBeenCalled();
  });

  test.each([
    [new ApiError(400), 'Revisa los datos del formulario.'],
    [new ApiError(401), 'Tu sesión ha caducado. Inicia sesión de nuevo.'],
    [new ApiError(403), 'No puedes modificar este viaje.'],
    [new ApiError(404), 'Este viaje ya no existe.'],
    [new NetworkError(), 'No se pudo conectar con el servidor. Inténtalo de nuevo.'],
    [new ApiError(500), 'Algo ha fallado en el servidor. Inténtalo de nuevo más tarde.'],
  ])('shows the failure inline and stays on the form (%#)', async (failure, message) => {
    vi.mocked(createTrip).mockRejectedValue(failure);
    const router = renderRoutes(routes, '/trips/new');
    fillRequired();

    fireEvent.click(screen.getByRole('button', { name: 'Crear viaje' }));

    expect(await screen.findByText(message)).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/trips/new');
    expect(screen.getByRole('button', { name: 'Crear viaje' })).toBeEnabled();
    expect(applyCover).not.toHaveBeenCalled();
  });

  test('disables the buttons while the trip is being saved', async () => {
    let finish: (trip: ReturnType<typeof tripOf>) => void = () => {};
    vi.mocked(createTrip).mockReturnValue(
      new Promise((resolve) => {
        finish = resolve;
      }),
    );
    renderRoutes(routes, '/trips/new');
    fillRequired();

    fireEvent.click(screen.getByRole('button', { name: 'Crear viaje' }));

    expect(await screen.findByRole('button', { name: 'Guardando…' })).toBeDisabled();
    finish(tripOf({ id: 9 }));
    expect(await screen.findByText('Detalle del viaje')).toBeInTheDocument();
  });

  test('when only the cover fails the trip is kept and its edit page explains it', async () => {
    vi.mocked(createTrip).mockResolvedValue(tripOf({ id: 9 }));
    vi.mocked(applyCover).mockRejectedValue(new ApiError(413));
    vi.mocked(getTrip).mockResolvedValue(tripOf({ id: 9 }));
    const router = renderRoutes(routes, '/trips/new');
    fillRequired();

    fireEvent.click(screen.getByRole('button', { name: 'Crear viaje' }));

    expect(
      await screen.findByText('El viaje se ha guardado, pero no se pudo actualizar la portada', {
        exact: false,
      }),
    ).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/trips/9/edit');
    expect(screen.getByRole('heading', { name: 'Editar viaje' })).toBeInTheDocument();
  });

  test('cancel goes back to the list', () => {
    const router = renderRoutes(routes, '/trips/new');

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(router.state.location.pathname).toBe('/trips');
  });
});

describe('B4 editing a trip', () => {
  const trip = tripOf({ id: 5, name: 'Verano', destination: 'Roma' });

  test('shows a loading text and then the form with the data of the trip', async () => {
    vi.mocked(getTrip).mockResolvedValue(trip);
    renderRoutes(routes, '/trips/5/edit');

    expect(screen.getByRole('status')).toHaveTextContent('Cargando viaje…');
    expect(await screen.findByLabelText('Nombre del viaje')).toHaveValue('Verano');
    expect(screen.getByRole('heading', { name: 'Editar viaje' })).toBeInTheDocument();
    expect(getTrip).toHaveBeenCalledWith(5);
  });

  test('replaces the trip, applies the cover change and opens the trip', async () => {
    vi.mocked(getTrip).mockResolvedValue(trip);
    vi.mocked(updateTrip).mockResolvedValue(trip);
    const router = renderRoutes(routes, '/trips/5/edit');
    await screen.findByLabelText('Nombre del viaje');
    type('Nombre del viaje', 'Verano 2027');

    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => expect(router.state.location.pathname).toBe('/trips/5'));
    expect(updateTrip).toHaveBeenCalledWith(5, expect.objectContaining({ name: 'Verano 2027' }));
    expect(applyCover).toHaveBeenCalledWith(5, { kind: 'keep' });
    expect(createTrip).not.toHaveBeenCalled();
  });

  test('a failed save is shown inline', async () => {
    vi.mocked(getTrip).mockResolvedValue(trip);
    vi.mocked(updateTrip).mockRejectedValue(new ApiError(403));
    renderRoutes(routes, '/trips/5/edit');
    await screen.findByLabelText('Nombre del viaje');

    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByText('No puedes modificar este viaje.')).toBeInTheDocument();
  });

  test('cancel goes back to the trip', async () => {
    vi.mocked(getTrip).mockResolvedValue(trip);
    const router = renderRoutes(routes, '/trips/5/edit');
    await screen.findByLabelText('Nombre del viaje');

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(router.state.location.pathname).toBe('/trips/5');
  });

  test('a trip that cannot be loaded goes to the route error page', async () => {
    vi.mocked(getTrip).mockRejectedValue(new ApiError(403));
    renderRoutes(routes, '/trips/5/edit');

    expect(await screen.findByText('Página de error')).toBeInTheDocument();
    expect(screen.queryByLabelText('Nombre del viaje')).not.toBeInTheDocument();
  });

  test('shows the notice sent by the page that saved the trip', async () => {
    vi.mocked(getTrip).mockResolvedValue(trip);
    renderRoutes(routes, '/trips/5/edit', undefined, { notice: 'Aviso de portada.' });

    expect(await screen.findByText('Aviso de portada.')).toBeInTheDocument();
  });
});
