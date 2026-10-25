import { useState } from 'react';
import { ApiError, NetworkError } from '../services/apiError';
import { deleteTrip } from '../services/tripService';
import type { Trip } from '../types';
import ConfirmModal from './ConfirmModal';

function deleteErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 401) return 'Tu sesión ha caducado. Inicia sesión de nuevo.';
    if (error.status === 403) return 'No puedes eliminar este viaje.';
  }
  if (error instanceof NetworkError) {
    return 'No se pudo conectar con el servidor. Inténtalo de nuevo.';
  }
  return 'No se pudo eliminar el viaje. Inténtalo de nuevo.';
}

interface DeleteTripModalProps {
  trip: Trip;
  show: boolean;
  onCancel: () => void;
  /** Called once the trip no longer exists, deleted now or before. */
  onDeleted: () => void;
}

/** Asks for confirmation and deletes the trip with its cover. */
export default function DeleteTripModal({
  trip,
  show,
  onCancel,
  onDeleted,
}: Readonly<DeleteTripModalProps>) {
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
      await deleteTrip(trip.id);
      onDeleted();
    } catch (failure) {
      // A trip that is already gone is what the user wanted.
      if (failure instanceof ApiError && failure.status === 404) onDeleted();
      else setError(deleteErrorMessage(failure));
    } finally {
      setBusy(false);
    }
  }

  return (
    <ConfirmModal
      show={show}
      title="Eliminar viaje"
      confirmLabel="Eliminar viaje"
      busyLabel="Eliminando…"
      busy={busy}
      error={error}
      onConfirm={confirm}
      onCancel={cancel}
    >
      Se eliminará «{trip.name}» con su portada. Esta acción no se puede deshacer.
    </ConfirmModal>
  );
}
