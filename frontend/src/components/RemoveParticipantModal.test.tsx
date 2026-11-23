import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { ApiError, NetworkError } from '../services/apiError';
import { removeParticipant } from '../services/participantService';
import { participantOf } from '../testHelpers';
import RemoveParticipantModal from './RemoveParticipantModal';

vi.mock('../services/participantService', () => ({ removeParticipant: vi.fn() }));

const onCancel = vi.fn();
const onRemoved = vi.fn();

beforeEach(() => {
  vi.mocked(removeParticipant).mockReset();
  onCancel.mockReset();
  onRemoved.mockReset();
});

function renderModal() {
  render(
    <RemoveParticipantModal
      tripId={5}
      participant={participantOf({ userId: 8, name: 'Bob' })}
      onCancel={onCancel}
      onRemoved={onRemoved}
    />,
  );
}

function confirm() {
  fireEvent.click(screen.getByRole('button', { name: 'Quitar del viaje' }));
}

describe('I3 RemoveParticipantModal', () => {
  test('says who is removed and that they can be invited again', () => {
    renderModal();

    expect(screen.getByRole('heading', { name: 'Quitar participante' })).toBeInTheDocument();
    expect(
      screen.getByText('Se quitará a Bob del viaje. Podrás volver a invitar a esta persona.'),
    ).toBeInTheDocument();
  });

  test('cancelling removes nothing', () => {
    renderModal();

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(onCancel).toHaveBeenCalled();
    expect(removeParticipant).not.toHaveBeenCalled();
  });

  test('confirming removes that user of that trip, shows progress meanwhile and reports it', async () => {
    let finish: () => void = () => undefined;
    vi.mocked(removeParticipant).mockReturnValue(
      new Promise<void>((resolve) => {
        finish = resolve;
      }),
    );
    renderModal();

    confirm();

    expect(await screen.findByRole('button', { name: 'Quitando…' })).toBeDisabled();
    expect(removeParticipant).toHaveBeenCalledWith(5, 8);
    finish();
    await vi.waitFor(() => expect(onRemoved).toHaveBeenCalledTimes(1));
  });

  test('somebody who is already gone counts as removed', async () => {
    vi.mocked(removeParticipant).mockRejectedValue(new ApiError(404));
    renderModal();

    confirm();

    await vi.waitFor(() => expect(onRemoved).toHaveBeenCalledTimes(1));
  });

  test.each([
    [new ApiError(401), 'Tu sesión ha caducado. Inicia sesión de nuevo.'],
    [new ApiError(403), 'No puedes quitar a esta persona del viaje.'],
    [new NetworkError(), 'No se pudo conectar con el servidor. Inténtalo de nuevo.'],
    [new ApiError(500), 'No se pudo quitar a la persona del viaje. Inténtalo de nuevo.'],
  ])('shows %s in the modal, which stays open to try again', async (failure, message) => {
    vi.mocked(removeParticipant).mockRejectedValue(failure);
    renderModal();

    confirm();

    expect(await screen.findByText(message)).toBeVisible();
    expect(onRemoved).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Quitar del viaje' })).toBeEnabled();
  });
});
