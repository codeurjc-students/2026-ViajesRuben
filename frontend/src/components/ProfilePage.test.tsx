import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import ProfilePage from './ProfilePage';
import { AuthContext, type AuthContextValue } from '../context/AuthContext';
import { ApiError, NetworkError } from '../services/apiError';
import {
  changePassword,
  deleteAvatar,
  getProfile,
  updateProfile,
  uploadAvatar,
} from '../services/userService';
import { ANA, authValue } from '../testHelpers';
import type { User } from '../types';

vi.mock('../services/userService');

const WITH_AVATAR: User = { ...ANA, avatarId: 'abc-123' };
const ADMIN: User = { ...ANA, id: 9, name: 'Administrator', role: 'ADMIN' };
const STORED = { id: 'new-avatar', contentType: 'image/png', size: 4 };

function renderProfile(auth: AuthContextValue = authValue(ANA)) {
  render(
    <AuthContext.Provider value={auth}>
      <MemoryRouter>
        <ProfilePage />
      </MemoryRouter>
    </AuthContext.Provider>,
  );
  return auth;
}

/** Renders the page once the profile has loaded. */
async function renderLoaded(profile: User = ANA, auth: AuthContextValue = authValue(profile)) {
  vi.mocked(getProfile).mockResolvedValue(profile);
  renderProfile(auth);
  await screen.findByText('Tus datos');
  return auth;
}

function type(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } });
}

function click(name: string) {
  fireEvent.click(screen.getByRole('button', { name }));
}

function png() {
  return new File([new Uint8Array(4)], 'a.png', { type: 'image/png' });
}

beforeEach(() => {
  vi.resetAllMocks();
});

describe('B3 ProfilePage loading', () => {
  test('shows a loading message and then the profile of the logged-in user', async () => {
    vi.mocked(getProfile).mockResolvedValue(ANA);

    renderProfile();

    expect(screen.getByRole('heading', { name: 'Mi perfil' })).toBeInTheDocument();
    expect(screen.getByText('Cargando perfil…')).toBeInTheDocument();
    expect(await screen.findByText('ana@example.com')).toBeInTheDocument();
    expect(getProfile).toHaveBeenCalledWith(ANA.id);
    expect(screen.getByText('Ana')).toBeInTheDocument();
    expect(screen.getByText('1 de octubre de 2026')).toBeInTheDocument();
    expect(screen.queryByText('Cargando perfil…')).not.toBeInTheDocument();
  });

  test.each([
    [new ApiError(403), 'No puedes modificar este perfil.'],
    [new ApiError(401), 'Tu sesión ha caducado. Inicia sesión de nuevo.'],
    [new NetworkError(), 'No se pudo conectar con el servidor. Inténtalo de nuevo.'],
  ])('shows %s as an inline message', async (failure, message) => {
    vi.mocked(getProfile).mockRejectedValue(failure);

    renderProfile();

    expect(await screen.findByRole('alert')).toHaveTextContent(message);
    expect(screen.queryByText('Tus datos')).not.toBeInTheDocument();
  });

  test('shows nothing to load when nobody is logged in', () => {
    renderProfile(authValue(null));

    expect(getProfile).not.toHaveBeenCalled();
  });
});

describe('B3 ProfilePage details', () => {
  test('edit mode starts with the current values and Cancel goes back', async () => {
    await renderLoaded();

    click('Editar datos');

    expect(screen.getByLabelText('Nombre')).toHaveValue('Ana');
    expect(screen.getByLabelText('Email')).toHaveValue('ana@example.com');
    type('Nombre', 'Otra');
    click('Cancelar');
    expect(screen.getByText('Ana')).toBeInTheDocument();
    click('Editar datos');
    expect(screen.getByLabelText('Nombre')).toHaveValue('Ana');
  });

  test('does not send an empty name or an invalid email', async () => {
    await renderLoaded();
    click('Editar datos');

    type('Nombre', '   ');
    type('Email', 'no-es-un-email');
    click('Guardar cambios');

    expect(await screen.findByText('Escribe tu nombre.')).toBeInTheDocument();
    expect(screen.getByText(/email válido/)).toBeInTheDocument();
    expect(updateProfile).not.toHaveBeenCalled();
  });

  test('saves the trimmed values, updates the auth context and returns to view mode', async () => {
    const auth = await renderLoaded();
    const updated: User = { ...ANA, name: 'Ana Ruiz', email: 'ana.r@example.com' };
    vi.mocked(updateProfile).mockResolvedValue(updated);
    click('Editar datos');

    type('Nombre', '  Ana Ruiz ');
    type('Email', ' ana.r@example.com ');
    click('Guardar cambios');

    expect(await screen.findByText('Perfil actualizado.')).toBeInTheDocument();
    expect(updateProfile).toHaveBeenCalledWith(ANA.id, {
      name: 'Ana Ruiz',
      email: 'ana.r@example.com',
    });
    expect(auth.updateUser).toHaveBeenCalledWith(updated);
    expect(screen.getByText('ana.r@example.com')).toBeInTheDocument();
    expect(screen.queryByLabelText('Nombre')).not.toBeInTheDocument();
  });

  test('a taken email is explained under the email field and edit mode stays open', async () => {
    const auth = await renderLoaded();
    vi.mocked(updateProfile).mockRejectedValue(new ApiError(409));
    click('Editar datos');

    type('Email', 'bob@example.com');
    click('Guardar cambios');

    expect(await screen.findByText('Ya existe una cuenta con ese email.')).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toHaveClass('is-invalid');
    expect(auth.updateUser).not.toHaveBeenCalled();
  });

  test.each([
    [new ApiError(500), 'Algo ha fallado en el servidor. Inténtalo de nuevo más tarde.'],
    [new ApiError(403), 'No puedes modificar este perfil.'],
    [new ApiError(401), 'Tu sesión ha caducado. Inicia sesión de nuevo.'],
    [new NetworkError(), 'No se pudo conectar con el servidor. Inténtalo de nuevo.'],
  ])('shows a failed save (%s) as an inline alert', async (failure, message) => {
    await renderLoaded();
    vi.mocked(updateProfile).mockRejectedValue(failure);
    click('Editar datos');

    click('Guardar cambios');

    expect(await screen.findByRole('alert')).toHaveTextContent(message);
  });

  test('disables the buttons while saving', async () => {
    await renderLoaded();
    vi.mocked(updateProfile).mockReturnValue(new Promise(() => {}));
    click('Editar datos');

    click('Guardar cambios');

    expect(await screen.findByRole('button', { name: 'Guardando…' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeDisabled();
  });

  test('entering edit mode again hides the previous success message', async () => {
    await renderLoaded();
    vi.mocked(updateProfile).mockResolvedValue(ANA);
    click('Editar datos');
    click('Guardar cambios');
    await screen.findByText('Perfil actualizado.');

    click('Editar datos');

    expect(screen.queryByText('Perfil actualizado.')).not.toBeInTheDocument();
  });
});

describe('B3 ProfilePage password', () => {
  function fillPasswords(current: string, next: string, confirmation = next) {
    type('Contraseña actual', current);
    type('Nueva contraseña', next);
    type('Confirmar nueva contraseña', confirmation);
  }

  test('has a separate form with three password fields', async () => {
    await renderLoaded();

    expect(screen.getByRole('heading', { name: 'Cambiar contraseña' })).toBeInTheDocument();
    for (const label of ['Contraseña actual', 'Nueva contraseña', 'Confirmar nueva contraseña']) {
      expect(screen.getByLabelText(label)).toHaveAttribute('type', 'password');
    }
  });

  test('explains each invalid field and sends nothing', async () => {
    await renderLoaded();

    click('Guardar contraseña');
    expect(await screen.findByText('Escribe tu contraseña actual.')).toBeInTheDocument();
    expect(screen.getByText('Escribe la nueva contraseña.')).toBeInTheDocument();
    expect(screen.getByText('Repite la nueva contraseña.')).toBeInTheDocument();

    fillPasswords('old-secret', 'short', 'other');
    click('Guardar contraseña');
    expect(
      await screen.findByText(/al menos 8 caracteres\.$/, { selector: '.invalid-feedback' }),
    ).toBeInTheDocument();
    expect(screen.getByText('Las contraseñas no coinciden.')).toBeInTheDocument();
    expect(changePassword).not.toHaveBeenCalled();
  });

  test('changes the password, confirms it and clears the form', async () => {
    await renderLoaded();
    vi.mocked(changePassword).mockResolvedValue(undefined);

    fillPasswords('old-secret', 'new-secret');
    click('Guardar contraseña');

    expect(await screen.findByText(/Contraseña cambiada/)).toBeInTheDocument();
    expect(changePassword).toHaveBeenCalledWith(ANA.id, {
      currentPassword: 'old-secret',
      newPassword: 'new-secret',
    });
    expect(screen.getByLabelText('Contraseña actual')).toHaveValue('');
    expect(screen.getByLabelText('Nueva contraseña')).toHaveValue('');
    expect(screen.getByLabelText('Confirmar nueva contraseña')).toHaveValue('');
  });

  test('a wrong current password is explained under that field and keeps the form', async () => {
    await renderLoaded();
    vi.mocked(changePassword).mockRejectedValue(new ApiError(400));

    fillPasswords('wrong-pass', 'new-secret');
    click('Guardar contraseña');

    expect(await screen.findByText('La contraseña actual no es correcta.')).toBeInTheDocument();
    expect(screen.getByLabelText('Contraseña actual')).toHaveClass('is-invalid');
    expect(screen.getByLabelText('Contraseña actual')).toHaveValue('wrong-pass');
    expect(screen.queryByText(/Contraseña cambiada/)).not.toBeInTheDocument();
  });

  test.each([
    [new ApiError(401), 'Tu sesión ha caducado. Inicia sesión de nuevo.'],
    [new ApiError(403), 'No puedes modificar este perfil.'],
    [new NetworkError(), 'No se pudo conectar con el servidor. Inténtalo de nuevo.'],
  ])('shows another failure (%s) as an inline alert', async (failure, message) => {
    await renderLoaded();
    vi.mocked(changePassword).mockRejectedValue(failure);

    fillPasswords('old-secret', 'new-secret');
    click('Guardar contraseña');

    expect(await screen.findByRole('alert')).toHaveTextContent(message);
  });

  test('disables the button while changing', async () => {
    await renderLoaded();
    vi.mocked(changePassword).mockReturnValue(new Promise(() => {}));

    fillPasswords('old-secret', 'new-secret');
    click('Guardar contraseña');

    expect(await screen.findByRole('button', { name: 'Cambiando…' })).toBeDisabled();
  });
});

describe('B3 ProfilePage avatar', () => {
  test('without avatar shows the initial and offers to upload one', async () => {
    await renderLoaded();

    expect(screen.getByLabelText('Subir foto')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Eliminar foto' })).not.toBeInTheDocument();
    expect(screen.queryByAltText('Tu foto de perfil')).not.toBeInTheDocument();
  });

  test('with avatar shows the picture and offers to replace or delete it', async () => {
    await renderLoaded(WITH_AVATAR);

    expect(screen.getByAltText('Tu foto de perfil')).toHaveAttribute(
      'src',
      '/api/v1/images/abc-123',
    );
    expect(screen.getByLabelText('Cambiar foto')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Eliminar foto' })).toBeInTheDocument();
  });

  test('uploads the chosen picture for this user and updates the auth context', async () => {
    const auth = await renderLoaded();
    vi.mocked(uploadAvatar).mockResolvedValue(STORED);
    const file = png();

    fireEvent.change(screen.getByLabelText('Subir foto'), { target: { files: [file] } });

    await waitFor(() =>
      expect(auth.updateUser).toHaveBeenCalledWith({ ...ANA, avatarId: 'new-avatar' }),
    );
    expect(uploadAvatar).toHaveBeenCalledWith(ANA.id, file);
    expect(await screen.findByLabelText('Cambiar foto')).toBeInTheDocument();
    expect(screen.getByAltText('Tu foto de perfil')).toHaveAttribute(
      'src',
      '/api/v1/images/new-avatar',
    );
  });

  test('rejects a file that is not an image before sending it', async () => {
    await renderLoaded();
    const text = new File(['hola'], 'a.txt', { type: 'text/plain' });

    fireEvent.change(screen.getByLabelText('Subir foto'), { target: { files: [text] } });

    expect(await screen.findByRole('alert')).toHaveTextContent('no es una imagen válida');
    expect(uploadAvatar).not.toHaveBeenCalled();
  });

  test('explains a rejected upload', async () => {
    await renderLoaded();
    vi.mocked(uploadAvatar).mockRejectedValue(new ApiError(413));

    fireEvent.change(screen.getByLabelText('Subir foto'), { target: { files: [png()] } });

    expect(await screen.findByRole('alert')).toHaveTextContent('supera los 5 MB');
  });

  test('deletes the avatar and updates the auth context', async () => {
    const auth = await renderLoaded(WITH_AVATAR);
    vi.mocked(deleteAvatar).mockResolvedValue(undefined);

    click('Eliminar foto');

    await waitFor(() =>
      expect(auth.updateUser).toHaveBeenCalledWith({ ...WITH_AVATAR, avatarId: null }),
    );
    expect(deleteAvatar).toHaveBeenCalledWith(ANA.id);
    expect(await screen.findByLabelText('Subir foto')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Eliminar foto' })).not.toBeInTheDocument();
  });

  test('disables the delete button while deleting', async () => {
    await renderLoaded(WITH_AVATAR);
    vi.mocked(deleteAvatar).mockReturnValue(new Promise(() => {}));

    click('Eliminar foto');

    expect(await screen.findByRole('button', { name: 'Eliminando…' })).toBeDisabled();
  });

  test('keeps the avatar and explains a failed delete', async () => {
    const auth = await renderLoaded(WITH_AVATAR);
    vi.mocked(deleteAvatar).mockRejectedValue(new ApiError(500));

    click('Eliminar foto');

    expect(await screen.findByRole('alert')).toHaveTextContent('Algo ha fallado en el servidor');
    expect(auth.updateUser).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Eliminar foto' })).toBeEnabled();
  });
});

describe('B3 ProfilePage administrator', () => {
  test('cannot edit the details or the password, only the avatar', async () => {
    await renderLoaded(ADMIN);

    expect(screen.queryByRole('button', { name: 'Editar datos' })).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Cambiar contraseña' })).not.toBeInTheDocument();
    expect(screen.getByText(/se gestionan en la configuración/)).toBeInTheDocument();
    expect(screen.getByLabelText('Subir foto')).toBeInTheDocument();
  });
});
