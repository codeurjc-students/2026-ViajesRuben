import { fireEvent, render, screen } from '@testing-library/react';
import { expect, test, vi } from 'vitest';
import ConfirmModal from './ConfirmModal';

function renderModal(props: Partial<Parameters<typeof ConfirmModal>[0]> = {}) {
  const onConfirm = vi.fn();
  const onCancel = vi.fn();
  render(
    <ConfirmModal
      show
      title="Eliminar viaje"
      confirmLabel="Eliminar"
      busyLabel="Eliminando…"
      busy={false}
      onConfirm={onConfirm}
      onCancel={onCancel}
      {...props}
    >
      Se borrará el viaje a Lisboa.
    </ConfirmModal>,
  );
  return { onConfirm, onCancel };
}

test('B4 shows the title, the explanation and both buttons in a dialog', () => {
  renderModal();

  const dialog = screen.getByRole('dialog', { name: 'Eliminar viaje' });
  expect(dialog).toHaveTextContent('Se borrará el viaje a Lisboa.');
  expect(screen.getByRole('button', { name: 'Eliminar' })).toBeEnabled();
  expect(screen.getByRole('button', { name: 'Cancelar' })).toBeEnabled();
});

test('B4 confirm and cancel call their handlers', () => {
  const { onConfirm, onCancel } = renderModal();

  fireEvent.click(screen.getByRole('button', { name: 'Eliminar' }));
  expect(onConfirm).toHaveBeenCalledTimes(1);
  fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));
  expect(onCancel).toHaveBeenCalledTimes(1);
});

test('B4 closing with the X or Escape cancels', () => {
  const { onCancel } = renderModal();

  fireEvent.click(screen.getByRole('button', { name: /close/i }));
  fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape', keyCode: 27 });

  expect(onCancel).toHaveBeenCalledTimes(2);
});

test('B4 while busy both buttons are disabled, the label changes and Escape does nothing', () => {
  const { onCancel } = renderModal({ busy: true });

  expect(screen.getByRole('button', { name: 'Eliminando…' })).toBeDisabled();
  expect(screen.getByRole('button', { name: 'Cancelar' })).toBeDisabled();
  expect(screen.queryByRole('button', { name: /close/i })).not.toBeInTheDocument();
  fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape', keyCode: 27 });
  expect(onCancel).not.toHaveBeenCalled();
});

test('B4 shows an error inside the modal and keeps it open', () => {
  renderModal({ error: 'No se pudo eliminar el viaje.' });

  expect(screen.getByRole('alert')).toHaveTextContent('No se pudo eliminar el viaje.');
  expect(screen.getByRole('dialog')).toBeInTheDocument();
});

test('B4 renders nothing when hidden', () => {
  renderModal({ show: false });

  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});
