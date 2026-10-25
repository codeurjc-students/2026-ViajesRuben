import { useId, type ReactNode } from 'react';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Modal from 'react-bootstrap/Modal';

interface ConfirmModalProps {
  show: boolean;
  title: string;
  /** What is about to happen, and what it affects. */
  children: ReactNode;
  confirmLabel: string;
  /** Replaces the confirm label while `busy`. */
  busyLabel: string;
  busy: boolean;
  /** Why the action failed; the modal stays open so the user can try again or cancel. */
  error?: string;
  onConfirm: () => void;
  onCancel: () => void;
}

/** Confirmation of a destructive action. It cannot be dismissed while the action is running. */
export default function ConfirmModal({
  show,
  title,
  children,
  confirmLabel,
  busyLabel,
  busy,
  error,
  onConfirm,
  onCancel,
}: Readonly<ConfirmModalProps>) {
  const titleId = useId();
  return (
    <Modal
      show={show}
      onHide={onCancel}
      centered
      aria-labelledby={titleId}
      backdrop={busy ? 'static' : true}
      keyboard={!busy}
    >
      <Modal.Header closeButton={!busy}>
        <Modal.Title as="h2" className="h4" id={titleId}>
          {title}
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {children}
        {error && (
          <Alert variant="danger" className="mt-3 mb-0">
            {error}
          </Alert>
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="outline-secondary" disabled={busy} onClick={onCancel}>
          Cancelar
        </Button>
        <Button variant="danger" disabled={busy} onClick={onConfirm}>
          {busy ? busyLabel : confirmLabel}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
