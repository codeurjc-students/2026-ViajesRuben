import { useState } from 'react';
import { ApiError, NetworkError } from '../services/apiError';
import { removeParticipant } from '../services/participantService';
import type { Participant } from '../types';
import ConfirmModal from './ConfirmModal';

function removeErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 401) return 'Tu sesión ha caducado. Inicia sesión de nuevo.';
    if (error.status === 403) return 'No puedes quitar a esta persona del viaje.';
  }
  if (error instanceof NetworkError) {
    return 'No se pudo conectar con el servidor. Inténtalo de nuevo.';
  }
  return 'No se pudo quitar a la persona del viaje. Inténtalo de nuevo.';
}

interface RemoveParticipantModalProps {
  tripId: number;
  participant: Participant;
  onCancel: () => void;
  /** Called once the person is no longer in the trip, removed now or before. */
  onRemoved: () => void;
}

/** Asks the creator for confirmation and removes the participant from the trip. */
export default function RemoveParticipantModal({
  tripId,
  participant,
  onCancel,
  onRemoved,
}: Readonly<RemoveParticipantModalProps>) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();

  async function confirm() {
    setBusy(true);
    setError(undefined);
    try {
      await removeParticipant(tripId, participant.userId);
      onRemoved();
    } catch (removeFailure) {
      // Someone who is already gone is what the creator wanted.
      if (removeFailure instanceof ApiError && removeFailure.status === 404) onRemoved();
      else setError(removeErrorMessage(removeFailure));
    } finally {
      setBusy(false);
    }
  }

  return (
    <ConfirmModal
      show
      title="Quitar participante"
      confirmLabel="Quitar del viaje"
      busyLabel="Quitando…"
      busy={busy}
      error={error}
      onConfirm={confirm}
      onCancel={onCancel}
    >
      {`Se quitará a ${participant.name} del viaje. Podrás volver a invitar a esta persona.`}
    </ConfirmModal>
  );
}
