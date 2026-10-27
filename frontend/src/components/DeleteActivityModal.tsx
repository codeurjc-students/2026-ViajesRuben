import { useState } from 'react';
import { ApiError, NetworkError } from '../services/apiError';
import { deleteActivity } from '../services/activityService';
import type { Activity } from '../types';
import ConfirmModal from './ConfirmModal';

function deleteErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 401) return 'Tu sesión ha caducado. Inicia sesión de nuevo.';
    if (error.status === 403) return 'No puedes eliminar esta actividad.';
  }
  if (error instanceof NetworkError) {
    return 'No se pudo conectar con el servidor. Inténtalo de nuevo.';
  }
  return 'No se pudo eliminar la actividad. Inténtalo de nuevo.';
}

interface DeleteActivityModalProps {
  activity: Activity;
  show: boolean;
  onCancel: () => void;
  /** Called once the activity no longer exists, deleted now or before. */
  onDeleted: () => void;
}

/** Asks for confirmation and deletes the activity with its image. */
export default function DeleteActivityModal({
  activity,
  show,
  onCancel,
  onDeleted,
}: Readonly<DeleteActivityModalProps>) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();

  function cancel() {
    setError(undefined);
    onCancel();
  }

  async function confirm() {
    setBusy(true);
    setError(undefined);
    try {
      await deleteActivity(activity.tripId, activity.id);
      onDeleted();
    } catch (failure) {
      // An activity that is already gone is what the user wanted.
      if (failure instanceof ApiError && failure.status === 404) onDeleted();
      else setError(deleteErrorMessage(failure));
    } finally {
      setBusy(false);
    }
  }

  return (
    <ConfirmModal
      show={show}
      title="Eliminar actividad"
      confirmLabel="Eliminar actividad"
      busyLabel="Eliminando…"
      busy={busy}
      error={error}
      onConfirm={confirm}
      onCancel={cancel}
    >
      Se eliminará «{activity.name}» con su imagen. Esta acción no se puede deshacer.
    </ConfirmModal>
  );
}
