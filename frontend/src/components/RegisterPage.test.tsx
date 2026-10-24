import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
import { test, expect, describe } from 'vitest';
import RegisterPage from './RegisterPage';
import { AuthContext, type AuthContextValue } from '../context/AuthContext';
import { ApiError, NetworkError } from '../services/apiError';
import { ANA, authValue } from '../testHelpers';

function renderRegister(auth: AuthContextValue, entry: string | object = '/register') {
  render(
    <AuthContext.Provider value={auth}>
      <MemoryRouter initialEntries={[entry]}>
        <Routes>
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/login" element={<p>Pantalla de login</p>} />
          <Route path="/trips" element={<p>Mis viajes</p>} />
          <Route path="/trips/7" element={<p>Detalle del viaje 7</p>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

function fill(name: string, email: string, password: string, confirmation = password) {
  fireEvent.change(screen.getByLabelText('Nombre'), { target: { value: name } });
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: email } });
  fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: password } });
  fireEvent.change(screen.getByLabelText('Confirmar contraseña'), {
    target: { value: confirmation },
  });
}

/** The submit button reads differently while the request is pending. */
const SUBMIT_LABELS = ['Crear cuenta', 'Creando cuenta…'];

function submit() {
  fireEvent.click(screen.getByRole('button', { name: (name) => SUBMIT_LABELS.includes(name) }));
}

describe('B2 RegisterPage', () => {
  test('shows the four fields and a link to log in', () => {
    renderRegister(authValue(null));

    expect(screen.getByRole('heading', { name: 'Crear cuenta' })).toBeInTheDocument();
    for (const label of ['Nombre', 'Email', 'Contraseña', 'Confirmar contraseña']) {
      expect(screen.getByLabelText(label)).toBeInTheDocument();
    }
    expect(screen.getByRole('link', { name: 'Inicia sesión' })).toHaveAttribute('href', '/login');
  });

  test('empty fields show a message each and nothing is sent', () => {
    const auth = authValue(null);
    renderRegister(auth);

    submit();

    expect(screen.getByText('Escribe tu nombre.')).toBeInTheDocument();
    expect(screen.getByText('Escribe tu email.')).toBeInTheDocument();
    expect(screen.getByText('Escribe una contraseña.')).toBeInTheDocument();
    expect(screen.getByText('Repite la contraseña.')).toBeInTheDocument();
    expect(auth.register).not.toHaveBeenCalled();
  });

  test('a password that is too short is rejected', () => {
    const auth = authValue(null);
    renderRegister(auth);

    fill('Ana', 'ana@example.com', 'corta');
    submit();

    expect(screen.getByText('La contraseña necesita al menos 8 caracteres.')).toBeInTheDocument();
    expect(auth.register).not.toHaveBeenCalled();
  });

  test('a confirmation that differs is rejected', () => {
    const auth = authValue(null);
    renderRegister(auth);

    fill('Ana', 'ana@example.com', 'secret-pass', 'other-pass');
    submit();

    expect(screen.getByText('Las contraseñas no coinciden.')).toBeInTheDocument();
    expect(auth.register).not.toHaveBeenCalled();
  });

  test('a name that is too long is rejected', () => {
    const auth = authValue(null);
    renderRegister(auth);

    fill('A'.repeat(101), 'ana@example.com', 'secret-pass');
    submit();

    expect(screen.getByText(/como máximo 100 caracteres/)).toBeInTheDocument();
    expect(auth.register).not.toHaveBeenCalled();
  });

  test('sends the trimmed name and email with the password', async () => {
    const auth = authValue(null);
    renderRegister(auth);

    fill('  Ana ', ' ana@example.com ', 'secret-pass');
    submit();

    await waitFor(() =>
      expect(auth.register).toHaveBeenCalledWith({
        name: 'Ana',
        email: 'ana@example.com',
        password: 'secret-pass',
      }),
    );
  });

  test('the button is disabled while the request is pending', async () => {
    const auth = authValue(null, { register: () => new Promise(() => {}) });
    renderRegister(auth);

    fill('Ana', 'ana@example.com', 'secret-pass');
    submit();

    expect(await screen.findByRole('button', { name: 'Creando cuenta…' })).toBeDisabled();
  });

  test('a duplicate email is shown under the email field', async () => {
    const auth = authValue(null, { register: () => Promise.reject(new ApiError(409)) });
    renderRegister(auth);

    fill('Ana', 'ana@example.com', 'secret-pass');
    submit();

    expect(await screen.findByText('Ya existe una cuenta con ese email.')).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toHaveClass('is-invalid');
    expect(screen.getByRole('button', { name: 'Crear cuenta' })).toBeEnabled();
  });

  test.each([
    [new ApiError(400), 'Revisa los datos del formulario'],
    [new ApiError(500), /Algo ha fallado en el servidor/],
    [new NetworkError(), /No se pudo conectar con el servidor/],
  ])('any other failure is shown in an alert (%#)', async (error, message) => {
    const auth = authValue(null, { register: () => Promise.reject(error) });
    renderRegister(auth);

    fill('Ana', 'ana@example.com', 'secret-pass');
    submit();

    expect(await screen.findByRole('alert')).toHaveTextContent(message);
    expect(screen.getByRole('button', { name: 'Crear cuenta' })).toBeEnabled();
  });

  test('a logged-in user (also right after registering) goes to the trip list', () => {
    renderRegister(authValue(ANA));

    expect(screen.getByText('Mis viajes')).toBeInTheDocument();
  });

  test('returns to the page the visitor came from', () => {
    renderRegister(authValue(ANA), {
      pathname: '/register',
      state: { from: { pathname: '/trips/7', search: '', hash: '' } },
    });

    expect(screen.getByText('Detalle del viaje 7')).toBeInTheDocument();
  });
});
