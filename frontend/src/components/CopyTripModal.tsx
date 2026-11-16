import { useId, useState, type SubmitEvent } from 'react';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import Modal from 'react-bootstrap/Modal';
import { ApiError, NetworkError } from '../services/apiError';
import { copyPublicTrip } from '../services/publicTripService';
import type { Trip } from '../types';

function copyErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 401) return 'Tu sesión ha caducado. Inicia sesión de nuevo.';
    if (error.status === 404) return 'Este itinerario ya no es público.';
  }
  if (error instanceof NetworkError) {
    return 'No se pudo conectar con el servidor. Inténtalo de nuevo.';
  }
  return 'No se pudo copiar el itinerario. Inténtalo de nuevo.';
}

interface CopyTripModalProps {
  /** The public trip to copy. */
  tripId: number;
  show: boolean;
  onCancel: () => void;
  /** Called with the new trip once the copy exists. */
  onCopied: (copy: Trip) => void;
}

/** Copies a public itinerary into the user's trips; the start date is optional. */
export default function CopyTripModal({
  tripId,
  show,
  onCancel,
  onCopied,
}: Readonly<CopyTripModalProps>) {
  const titleId = useId();
  const [startDate, setStartDate] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();

  function cancel() {
    setError(undefined);
    onCancel();
  }

  async function copy(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError(undefined);
    try {
      onCopied(await copyPublicTrip(tripId, startDate || undefined));
    } catch (failure) {
      setError(copyErrorMessage(failure));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      show={show}
      onHide={cancel}
      centered
      aria-labelledby={titleId}
      backdrop={busy ? 'static' : true}
      keyboard={!busy}
    >
      <Form onSubmit={copy}>
        <Modal.Header closeButton={!busy}>
          <Modal.Title as="h2" className="h4" id={titleId}>
            Copiar en tu cuenta
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p>
            Se creará un viaje nuevo con las actividades y las imágenes de este itinerario. Podrás
            editarlo libremente.
          </p>
          <Form.Group controlId="copy-start-date">
            <Form.Label>Nueva fecha de inicio (opcional)</Form.Label>
            <Form.Control
              type="date"
              value={startDate}
              disabled={busy}
              onChange={(event) => setStartDate(event.target.value)}
            />
            <Form.Text>Si la dejas vacía, se mantienen las fechas originales.</Form.Text>
          </Form.Group>
          {error && (
            <Alert variant="danger" className="mt-3 mb-0">
              {error}
            </Alert>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="outline-secondary" disabled={busy} onClick={cancel}>
            Cancelar
          </Button>
          <Button type="submit" disabled={busy}>
            {busy ? 'Copiando…' : 'Copiar viaje'}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}
