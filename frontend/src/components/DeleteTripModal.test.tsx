import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { ApiError, NetworkError } from '../services/apiError';
import { deleteTrip } from '../services/tripService';
import { tripOf } from '../testHelpers';
import DeleteTripModal from './DeleteTripModal';

vi.mock('../services/tripService', () => ({ deleteTrip: vi.fn() }));

const trip = tripOf({ id: 4, name: 'Escapada' });

function renderModal() {
  const onCancel = vi.fn();
  const onDeleted = vi.fn();
  render(<DeleteTripModal trip={trip} show onCancel={onCancel} onDeleted={onDeleted} />);
  return { onCancel, onDeleted };
}

beforeEach(() => {
  vi.mocked(deleteTrip).mockReset();
});

describe('B4 DeleteTripModal', () => {
  test('names the trip and warns that it cannot be undone', () => {
    renderModal();

    expect(screen.getByRole('dialog', { name: 'Eliminar viaje' })).toHaveTextContent(
      'Se eliminará «Escapada» con su portada y sus actividades. Esta acción no se puede deshacer.',
    );
  });

  test('deletes the trip on confirmation and reports it', async () => {
    vi.mocked(deleteTrip).mockResolvedValue(undefined);
    const { onDeleted } = renderModal();

    fireEvent.click(screen.getByRole('button', { name: 'Eliminar viaje' }));

    await waitFor(() => expect(onDeleted).toHaveBeenCalledTimes(1));
    expect(deleteTrip).toHaveBeenCalledWith(4);
  });

  test('shows the busy label while the request is running', async () => {
    let finish: () => void = () => {};
    vi.mocked(deleteTrip).mockReturnValue(
      new Promise<void>((resolve) => {
        finish = resolve;
      }),
    );
    renderModal();

    fireEvent.click(screen.getByRole('button', { name: 'Eliminar viaje' }));

    expect(await screen.findByRole('button', { name: 'Eliminando…' })).toBeDisabled();
    finish();
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Eliminar viaje' })).toBeEnabled(),
    );
  });

  test('cancelling does not delete anything', () => {
    const { onCancel } = renderModal();

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(deleteTrip).not.toHaveBeenCalled();
  });

  test('a trip that is already gone counts as deleted', async () => {
    vi.mocked(deleteTrip).mockRejectedValue(new ApiError(404));
    const { onDeleted } = renderModal();

    fireEvent.click(screen.getByRole('button', { name: 'Eliminar viaje' }));

    await waitFor(() => expect(onDeleted).toHaveBeenCalledTimes(1));
  });

  test.each([
    [new ApiError(401), 'Tu sesión ha caducado. Inicia sesión de nuevo.'],
    [new ApiError(403), 'No puedes eliminar este viaje.'],
    [new NetworkError(), 'No se pudo conectar con el servidor. Inténtalo de nuevo.'],
    [new ApiError(500), 'No se pudo eliminar el viaje. Inténtalo de nuevo.'],
  ])('keeps the modal open and explains the failure (%#)', async (failure, message) => {
    vi.mocked(deleteTrip).mockRejectedValue(failure);
    const { onDeleted } = renderModal();

    fireEvent.click(screen.getByRole('button', { name: 'Eliminar viaje' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(message);
    expect(onDeleted).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Eliminar viaje' })).toBeEnabled();
  });

  test('cancelling clears the previous error', async () => {
    vi.mocked(deleteTrip).mockRejectedValue(new ApiError(500));
    const { onCancel } = renderModal();
    fireEvent.click(screen.getByRole('button', { name: 'Eliminar viaje' }));
    await screen.findByRole('alert');

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(onCancel).toHaveBeenCalled();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
