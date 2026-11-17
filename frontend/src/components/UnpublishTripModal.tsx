import { useState } from 'react';
import { ApiError, NetworkError } from '../services/apiError';
import { setPublication } from '../services/tripService';
import ConfirmModal from './ConfirmModal';

function unpublishErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 401) return 'Tu sesión ha caducado. Inicia sesión de nuevo.';
    if (error.status === 403) return 'No puedes despublicar este viaje.';
  }
  if (error instanceof NetworkError) {
    return 'No se pudo conectar con el servidor. Inténtalo de nuevo.';
  }
  return 'No se pudo despublicar el viaje. Inténtalo de nuevo.';
}

interface UnpublishTripModalProps {
  trip: { id: number; name: string };
  show: boolean;
  onCancel: () => void;
  /** Called once the trip is no longer public, unpublished now or before. */
  onUnpublished: () => void;
}

/** Asks the administrator for confirmation and unpublishes the trip; the trip itself is kept. */
export default function UnpublishTripModal({
  trip,
  show,
  onCancel,
  onUnpublished,
}: Readonly<UnpublishTripModalProps>) {
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
      await setPublication(trip.id, false);
      onUnpublished();
    } catch (failure) {
      // A trip that is already gone is no longer public, which is what the user wanted.
      if (failure instanceof ApiError && failure.status === 404) onUnpublished();
      else setError(unpublishErrorMessage(failure));
    } finally {
      setBusy(false);
    }
  }

  return (
    <ConfirmModal
      show={show}
      title="Despublicar itinerario"
      confirmLabel="Despublicar"
      busyLabel="Despublicando…"
      busy={busy}
      error={error}
      onConfirm={confirm}
      onCancel={cancel}
    >
      {`«${trip.name}» dejará de ser público; el viaje no se borra.`}
    </ConfirmModal>
  );
}
