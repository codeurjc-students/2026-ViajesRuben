import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { ApiError, NetworkError } from '../services/apiError';
import { copyPublicTrip } from '../services/publicTripService';
import { tripOf } from '../testHelpers';
import CopyTripModal from './CopyTripModal';

vi.mock('../services/publicTripService', () => ({ copyPublicTrip: vi.fn() }));

const copy = tripOf({ id: 20, copiedFromId: 7 });

function renderModal() {
  const onCancel = vi.fn();
  const onCopied = vi.fn();
  render(<CopyTripModal tripId={7} show onCancel={onCancel} onCopied={onCopied} />);
  return { onCancel, onCopied };
}

function chooseDate(value: string) {
  fireEvent.change(screen.getByLabelText('Nueva fecha de inicio (opcional)'), {
    target: { value },
  });
}

beforeEach(() => {
  vi.mocked(copyPublicTrip).mockReset();
});

describe('I9 CopyTripModal', () => {
  test('explains the copy and that an empty date keeps the original dates', () => {
    renderModal();

    expect(screen.getByRole('dialog', { name: 'Copiar en tu cuenta' })).toBeInTheDocument();
    expect(screen.getByLabelText('Nueva fecha de inicio (opcional)')).toHaveValue('');
    expect(
      screen.getByText('Si la dejas vacía, se mantienen las fechas originales.'),
    ).toBeInTheDocument();
  });

  test('copies with the chosen start date and reports the new trip', async () => {
    vi.mocked(copyPublicTrip).mockResolvedValue(copy);
    const { onCopied } = renderModal();
    chooseDate('2027-09-01');

    fireEvent.click(screen.getByRole('button', { name: 'Copiar viaje' }));

    await waitFor(() => expect(onCopied).toHaveBeenCalledWith(copy));
    expect(copyPublicTrip).toHaveBeenCalledWith(7, '2027-09-01');
  });

  test('copies without a date when the field is left empty', async () => {
    vi.mocked(copyPublicTrip).mockResolvedValue(copy);
    const { onCopied } = renderModal();

    fireEvent.click(screen.getByRole('button', { name: 'Copiar viaje' }));

    await waitFor(() => expect(onCopied).toHaveBeenCalledWith(copy));
    expect(copyPublicTrip).toHaveBeenCalledWith(7, undefined);
  });

  test('is disabled while copying and enabled again afterwards', async () => {
    let finish: (trip: typeof copy) => void = () => {};
    vi.mocked(copyPublicTrip).mockReturnValue(
      new Promise((resolve) => {
        finish = resolve;
      }),
    );
    renderModal();

    fireEvent.click(screen.getByRole('button', { name: 'Copiar viaje' }));

    expect(await screen.findByRole('button', { name: 'Copiando…' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeDisabled();
    expect(screen.getByLabelText('Nueva fecha de inicio (opcional)')).toBeDisabled();
    finish(copy);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Copiar viaje' })).toBeEnabled());
  });

  test('cancelling does not copy anything', () => {
    const { onCancel } = renderModal();

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(copyPublicTrip).not.toHaveBeenCalled();
  });

  test.each([
    [new ApiError(401), 'Tu sesión ha caducado. Inicia sesión de nuevo.'],
    [new ApiError(404), 'Este itinerario ya no es público.'],
    [new NetworkError(), 'No se pudo conectar con el servidor. Inténtalo de nuevo.'],
    [new ApiError(500), 'No se pudo copiar el itinerario. Inténtalo de nuevo.'],
  ])('keeps the modal open and explains the failure (%#)', async (failure, message) => {
    vi.mocked(copyPublicTrip).mockRejectedValue(failure);
    const { onCopied } = renderModal();

    fireEvent.click(screen.getByRole('button', { name: 'Copiar viaje' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(message);
    expect(onCopied).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Copiar viaje' })).toBeEnabled();
  });

  test('cancelling clears the previous error', async () => {
    vi.mocked(copyPublicTrip).mockRejectedValue(new ApiError(500));
    const { onCancel } = renderModal();
    fireEvent.click(screen.getByRole('button', { name: 'Copiar viaje' }));
    await screen.findByRole('alert');

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(onCancel).toHaveBeenCalled();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
