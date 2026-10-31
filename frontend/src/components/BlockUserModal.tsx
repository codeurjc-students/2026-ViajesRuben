import { useState } from 'react';
import { ApiError, NetworkError } from '../services/apiError';
import { setUserStatus } from '../services/adminUserService';
import type { AdminUser } from '../types';
import ConfirmModal from './ConfirmModal';

function statusErrorMessage(error: unknown, blocking: boolean): string {
  if (error instanceof ApiError) {
    if (error.status === 401) return 'Tu sesión ha caducado. Inicia sesión de nuevo.';
    if (error.status === 403) return 'No puedes cambiar el estado de esta cuenta.';
  }
  if (error instanceof NetworkError) {
    return 'No se pudo conectar con el servidor. Inténtalo de nuevo.';
  }
  return blocking
    ? 'No se pudo bloquear la cuenta. Inténtalo de nuevo.'
    : 'No se pudo desbloquear la cuenta. Inténtalo de nuevo.';
}

interface BlockUserModalProps {
  user: AdminUser;
  onCancel: () => void;
  /** Called with the user as the API returns it, blocked or unblocked. */
  onChanged: (user: AdminUser) => void;
}

/** Asks for confirmation and blocks an active account, or unblocks a blocked one. */
export default function BlockUserModal({
  user,
  onCancel,
  onChanged,
}: Readonly<BlockUserModalProps>) {
  const blocking = user.status === 'ACTIVE';
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();

  async function confirm() {
    setBusy(true);
    setError(undefined);
    try {
      onChanged(await setUserStatus(user.id, blocking ? 'BLOCKED' : 'ACTIVE'));
    } catch (failure) {
      setError(statusErrorMessage(failure, blocking));
    } finally {
      setBusy(false);
    }
  }

  return (
    <ConfirmModal
      show
      title={blocking ? 'Bloquear cuenta' : 'Desbloquear cuenta'}
      confirmLabel={blocking ? 'Bloquear' : 'Desbloquear'}
      busyLabel={blocking ? 'Bloqueando…' : 'Desbloqueando…'}
      busy={busy}
      error={error}
      onConfirm={confirm}
      onCancel={onCancel}
    >
      {blocking
        ? `${user.name} no podrá iniciar sesión y su sesión abierta se cerrará en su próxima acción.`
        : `${user.name} volverá a poder iniciar sesión.`}
    </ConfirmModal>
  );
}
