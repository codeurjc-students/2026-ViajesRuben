import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { ApiError, NetworkError } from '../services/apiError';
import { setPublication } from '../services/tripService';
import UnpublishTripModal from './UnpublishTripModal';

vi.mock('../services/tripService', () => ({ setPublication: vi.fn() }));

const trip = { id: 4, name: 'Escapada' };

function renderModal() {
  const onCancel = vi.fn();
  const onUnpublished = vi.fn();
  render(<UnpublishTripModal trip={trip} show onCancel={onCancel} onUnpublished={onUnpublished} />);
  return { onCancel, onUnpublished };
}

beforeEach(() => {
  vi.mocked(setPublication).mockReset();
});

describe('I10 UnpublishTripModal', () => {
  test('names the trip and says it is kept', () => {
    renderModal();

    expect(screen.getByRole('dialog', { name: 'Despublicar itinerario' })).toHaveTextContent(
      '«Escapada» dejará de ser público; el viaje no se borra.',
    );
  });

  test('unpublishes the trip on confirmation and reports it', async () => {
    vi.mocked(setPublication).mockResolvedValue({ id: 4, published: false } as Awaited<
      ReturnType<typeof setPublication>
    >);
    const { onUnpublished } = renderModal();

    fireEvent.click(screen.getByRole('button', { name: 'Despublicar' }));

    await waitFor(() => expect(onUnpublished).toHaveBeenCalledTimes(1));
    expect(setPublication).toHaveBeenCalledWith(4, false);
  });

  test('shows the busy label while the request is running', async () => {
    let finish: () => void = () => {};
    vi.mocked(setPublication).mockReturnValue(
      new Promise((resolve) => {
        finish = () => resolve({ id: 4 } as Awaited<ReturnType<typeof setPublication>>);
      }),
    );
    renderModal();

    fireEvent.click(screen.getByRole('button', { name: 'Despublicar' }));

    expect(await screen.findByRole('button', { name: 'Despublicando…' })).toBeDisabled();
    finish();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Despublicar' })).toBeEnabled());
  });

  test('cancelling does not unpublish anything', () => {
    const { onCancel } = renderModal();

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(setPublication).not.toHaveBeenCalled();
  });

  test('a trip that is already gone counts as unpublished', async () => {
    vi.mocked(setPublication).mockRejectedValue(new ApiError(404));
    const { onUnpublished } = renderModal();

    fireEvent.click(screen.getByRole('button', { name: 'Despublicar' }));

    await waitFor(() => expect(onUnpublished).toHaveBeenCalledTimes(1));
  });

  test.each([
    [new ApiError(401), 'Tu sesión ha caducado. Inicia sesión de nuevo.'],
    [new ApiError(403), 'No puedes despublicar este viaje.'],
    [new NetworkError(), 'No se pudo conectar con el servidor. Inténtalo de nuevo.'],
    [new ApiError(500), 'No se pudo despublicar el viaje. Inténtalo de nuevo.'],
  ])('keeps the modal open and explains the failure (%#)', async (failure, message) => {
    vi.mocked(setPublication).mockRejectedValue(failure);
    const { onUnpublished } = renderModal();

    fireEvent.click(screen.getByRole('button', { name: 'Despublicar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(message);
    expect(onUnpublished).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Despublicar' })).toBeEnabled();
  });

  test('cancelling clears the previous error', async () => {
    vi.mocked(setPublication).mockRejectedValue(new ApiError(500));
    const { onCancel } = renderModal();
    fireEvent.click(screen.getByRole('button', { name: 'Despublicar' }));
    await screen.findByRole('alert');

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(onCancel).toHaveBeenCalled();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
