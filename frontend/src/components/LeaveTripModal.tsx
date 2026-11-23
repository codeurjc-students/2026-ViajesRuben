import { useState } from 'react';
import { ApiError, NetworkError } from '../services/apiError';
import { removeParticipant } from '../services/participantService';
import ConfirmModal from './ConfirmModal';

function leaveErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 401) {
    return 'Tu sesión ha caducado. Inicia sesión de nuevo.';
  }
  if (error instanceof NetworkError) {
    return 'No se pudo conectar con el servidor. Inténtalo de nuevo.';
  }
  return 'No se pudo salir del viaje. Inténtalo de nuevo.';
}

interface LeaveTripModalProps {
  tripId: number;
  tripName: string;
  /** The logged-in participant: a participant can only remove themselves. */
  userId: number;
  onCancel: () => void;
  /** Called once the user is no longer in the trip, left now or before. */
  onLeft: () => void;
}

/** Asks the participant for confirmation and leaves the trip. */
export default function LeaveTripModal({
  tripId,
  tripName,
  userId,
  onCancel,
  onLeft,
}: Readonly<LeaveTripModalProps>) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();

  async function confirm() {
    setBusy(true);
    setError(undefined);
    try {
      await removeParticipant(tripId, userId);
      onLeft();
    } catch (leaveFailure) {
      // Already out of the trip (the creator removed them meanwhile, or the trip is gone) is what
      // the user wanted.
      if (
        leaveFailure instanceof ApiError &&
        (leaveFailure.status === 403 || leaveFailure.status === 404)
      ) {
        onLeft();
      } else {
        setError(leaveErrorMessage(leaveFailure));
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <ConfirmModal
      show
      title="Salir del viaje"
      confirmLabel="Salir del viaje"
      busyLabel="Saliendo…"
      busy={busy}
      error={error}
      onConfirm={confirm}
      onCancel={onCancel}
    >
      {`Dejarás de ver «${tripName}». Para volver necesitarás una nueva invitación.`}
    </ConfirmModal>
  );
}
