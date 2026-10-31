import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import BlockUserModal from './BlockUserModal';
import { setUserStatus } from '../services/adminUserService';
import { ApiError, NetworkError } from '../services/apiError';
import { adminUserOf } from '../testHelpers';
import type { AdminUser } from '../types';

vi.mock('../services/adminUserService', () => ({ setUserStatus: vi.fn() }));

function renderModal(user: AdminUser) {
  const onCancel = vi.fn();
  const onChanged = vi.fn();
  render(<BlockUserModal user={user} onCancel={onCancel} onChanged={onChanged} />);
  return { onCancel, onChanged };
}

beforeEach(() => {
  vi.mocked(setUserStatus).mockReset();
});

describe('B7 BlockUserModal', () => {
  test('for an active user it explains the effect and blocks on confirmation', async () => {
    const ana = adminUserOf({ id: 5, name: 'Ana' });
    const blocked = { ...ana, status: 'BLOCKED' as const };
    vi.mocked(setUserStatus).mockResolvedValue(blocked);
    const { onChanged } = renderModal(ana);

    expect(screen.getByRole('dialog', { name: 'Bloquear cuenta' })).toHaveTextContent(
      'Ana no podrá iniciar sesión y su sesión abierta se cerrará en su próxima acción.',
    );
    fireEvent.click(screen.getByRole('button', { name: 'Bloquear' }));

    await waitFor(() => expect(onChanged).toHaveBeenCalledWith(blocked));
    expect(setUserStatus).toHaveBeenCalledWith(5, 'BLOCKED');
  });

  test('for a blocked user it unblocks on confirmation', async () => {
    const bob = adminUserOf({ id: 6, name: 'Bob', status: 'BLOCKED' });
    const active = { ...bob, status: 'ACTIVE' as const };
    vi.mocked(setUserStatus).mockResolvedValue(active);
    const { onChanged } = renderModal(bob);

    expect(screen.getByRole('dialog', { name: 'Desbloquear cuenta' })).toHaveTextContent(
      'Bob volverá a poder iniciar sesión.',
    );
    fireEvent.click(screen.getByRole('button', { name: 'Desbloquear' }));

    await waitFor(() => expect(onChanged).toHaveBeenCalledWith(active));
    expect(setUserStatus).toHaveBeenCalledWith(6, 'ACTIVE');
  });

  test.each([
    [new ApiError(401), 'Tu sesión ha caducado. Inicia sesión de nuevo.'],
    [new ApiError(403), 'No puedes cambiar el estado de esta cuenta.'],
    [new NetworkError(), 'No se pudo conectar con el servidor. Inténtalo de nuevo.'],
    [new ApiError(500), 'No se pudo bloquear la cuenta. Inténtalo de nuevo.'],
  ])('a failure keeps the modal open and says what happened (%#)', async (failure, message) => {
    vi.mocked(setUserStatus).mockRejectedValue(failure);
    const { onChanged } = renderModal(adminUserOf());

    fireEvent.click(screen.getByRole('button', { name: 'Bloquear' }));

    expect(await screen.findByText(message)).toBeInTheDocument();
    expect(onChanged).not.toHaveBeenCalled();
  });

  test('a failed unblock uses the unblock wording', async () => {
    vi.mocked(setUserStatus).mockRejectedValue(new ApiError(500));
    renderModal(adminUserOf({ status: 'BLOCKED' }));

    fireEvent.click(screen.getByRole('button', { name: 'Desbloquear' }));

    expect(
      await screen.findByText('No se pudo desbloquear la cuenta. Inténtalo de nuevo.'),
    ).toBeInTheDocument();
  });

  test('Cancelar closes without calling the API', () => {
    const { onCancel } = renderModal(adminUserOf());

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(setUserStatus).not.toHaveBeenCalled();
  });
});
