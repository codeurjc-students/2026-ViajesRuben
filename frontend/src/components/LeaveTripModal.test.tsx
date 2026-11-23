import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { ApiError, NetworkError } from '../services/apiError';
import { removeParticipant } from '../services/participantService';
import LeaveTripModal from './LeaveTripModal';

vi.mock('../services/participantService', () => ({ removeParticipant: vi.fn() }));

const onCancel = vi.fn();
const onLeft = vi.fn();

beforeEach(() => {
  vi.mocked(removeParticipant).mockReset();
  onCancel.mockReset();
  onLeft.mockReset();
});

function renderModal() {
  render(
    <LeaveTripModal
      tripId={5}
      tripName="Verano en Roma"
      userId={8}
      onCancel={onCancel}
      onLeft={onLeft}
    />,
  );
}

function confirm() {
  fireEvent.click(screen.getByRole('button', { name: 'Salir del viaje' }));
}

describe('I3 LeaveTripModal', () => {
  test('says which trip is left and that a new invitation is needed to come back', () => {
    renderModal();

    expect(screen.getByRole('heading', { name: 'Salir del viaje' })).toBeInTheDocument();
    expect(
      screen.getByText(
        'Dejarás de ver «Verano en Roma». Para volver necesitarás una nueva invitación.',
      ),
    ).toBeInTheDocument();
  });

  test('cancelling leaves nothing', () => {
    renderModal();

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(onCancel).toHaveBeenCalled();
    expect(removeParticipant).not.toHaveBeenCalled();
  });

  test('confirming removes the logged-in user from the trip, shows progress and reports it', async () => {
    let finish: () => void = () => undefined;
    vi.mocked(removeParticipant).mockReturnValue(
      new Promise<void>((resolve) => {
        finish = resolve;
      }),
    );
    renderModal();

    confirm();

    expect(await screen.findByRole('button', { name: 'Saliendo…' })).toBeDisabled();
    expect(removeParticipant).toHaveBeenCalledWith(5, 8);
    finish();
    await vi.waitFor(() => expect(onLeft).toHaveBeenCalledTimes(1));
  });

  test.each([403, 404])(
    'a %i means the user is already out of the trip, which is what they wanted',
    async (status) => {
      vi.mocked(removeParticipant).mockRejectedValue(new ApiError(status));
      renderModal();

      confirm();

      await vi.waitFor(() => expect(onLeft).toHaveBeenCalledTimes(1));
    },
  );

  test.each([
    [new ApiError(401), 'Tu sesión ha caducado. Inicia sesión de nuevo.'],
    [new NetworkError(), 'No se pudo conectar con el servidor. Inténtalo de nuevo.'],
    [new ApiError(500), 'No se pudo salir del viaje. Inténtalo de nuevo.'],
  ])('shows %s in the modal, which stays open to try again', async (failure, message) => {
    vi.mocked(removeParticipant).mockRejectedValue(failure);
    renderModal();

    confirm();

    expect(await screen.findByText(message)).toBeVisible();
    expect(onLeft).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Salir del viaje' })).toBeEnabled();
  });
});
