import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import EditUserModal from './EditUserModal';
import { updateUser } from '../services/adminUserService';
import { ApiError, NetworkError } from '../services/apiError';
import { adminUserOf } from '../testHelpers';

vi.mock('../services/adminUserService', () => ({ updateUser: vi.fn() }));

const user = adminUserOf({ id: 5, name: 'Ana', email: 'ana@example.com' });

function renderModal() {
  const onCancel = vi.fn();
  const onSaved = vi.fn();
  render(<EditUserModal user={user} onCancel={onCancel} onSaved={onSaved} />);
  return { onCancel, onSaved };
}

function fill(name: string, email: string) {
  fireEvent.change(screen.getByLabelText('Nombre'), { target: { value: name } });
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: email } });
}

function save() {
  fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));
}

beforeEach(() => {
  vi.mocked(updateUser).mockReset();
});

describe('B7 EditUserModal', () => {
  test('starts with the current name and email', () => {
    renderModal();

    expect(screen.getByLabelText('Nombre')).toHaveValue('Ana');
    expect(screen.getByLabelText('Email')).toHaveValue('ana@example.com');
  });

  test('saves the trimmed values and reports the updated user', async () => {
    const updated = { ...user, name: 'Ana Ruiz' };
    vi.mocked(updateUser).mockResolvedValue(updated);
    const { onSaved } = renderModal();

    fill('  Ana Ruiz ', ' ana@example.com ');
    save();

    await waitFor(() => expect(onSaved).toHaveBeenCalledWith(updated));
    expect(updateUser).toHaveBeenCalledWith(5, { name: 'Ana Ruiz', email: 'ana@example.com' });
  });

  test('validates like the profile form before calling the API', () => {
    renderModal();

    fill('', 'no-es-un-email');
    save();

    expect(screen.getByText('Escribe tu nombre.')).toBeInTheDocument();
    expect(screen.getByText(/Escribe un email válido/)).toBeInTheDocument();
    expect(updateUser).not.toHaveBeenCalled();
  });

  test('a taken email is reported under the email field', async () => {
    vi.mocked(updateUser).mockRejectedValue(new ApiError(409));
    const { onSaved } = renderModal();

    fill('Ana', 'bob@example.com');
    save();

    expect(await screen.findByText('Ya existe una cuenta con ese email.')).toBeInTheDocument();
    expect(onSaved).not.toHaveBeenCalled();
  });

  test('other failures are shown in the modal, which stays open', async () => {
    vi.mocked(updateUser).mockRejectedValue(new NetworkError());
    renderModal();

    save();

    expect(await screen.findByText(/No se pudo conectar con el servidor/)).toBeInTheDocument();
    expect(screen.getByRole('dialog', { name: 'Editar usuario' })).toBeInTheDocument();
  });

  test('disables the buttons and shows the busy label while saving', async () => {
    let finish: (value: typeof user) => void = () => {};
    vi.mocked(updateUser).mockReturnValue(
      new Promise((resolve) => {
        finish = resolve;
      }),
    );
    renderModal();

    save();

    expect(await screen.findByRole('button', { name: 'Guardando…' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeDisabled();
    finish(user);
  });

  test('Cancelar closes without saving', () => {
    const { onCancel } = renderModal();

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(updateUser).not.toHaveBeenCalled();
  });
});
