import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
import { test, expect, describe } from 'vitest';
import LoginPage from './LoginPage';
import { AuthContext, type AuthContextValue } from '../context/AuthContext';
import { ApiError, NetworkError } from '../services/apiError';
import { ANA, authValue } from '../testHelpers';

function renderLogin(auth: AuthContextValue, entry: string | object = '/login') {
  render(
    <AuthContext.Provider value={auth}>
      <MemoryRouter initialEntries={[entry]}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<p>Pantalla de registro</p>} />
          <Route path="/trips" element={<p>Mis viajes</p>} />
          <Route path="/trips/7" element={<p>Detalle del viaje 7</p>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

function fill(email: string, password: string) {
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: email } });
  fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: password } });
}

function submit() {
  fireEvent.click(screen.getByRole('button', { name: /Iniciar sesión|Entrando/ }));
}

describe('B2 LoginPage', () => {
  test('shows the form and a link to register', () => {
    renderLogin(authValue(null));

    expect(screen.getByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toBeInTheDocument();
    expect(screen.getByLabelText('Contraseña')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Regístrate' })).toHaveAttribute('href', '/register');
  });

  test('empty fields show messages and nothing is sent', () => {
    const auth = authValue(null);
    renderLogin(auth);

    submit();

    expect(screen.getByText('Escribe tu email.')).toBeInTheDocument();
    expect(screen.getByText('Escribe tu contraseña.')).toBeInTheDocument();
    expect(auth.login).not.toHaveBeenCalled();
  });

  test('an email without the right format is rejected before sending', () => {
    const auth = authValue(null);
    renderLogin(auth);

    fill('ana-sin-arroba', 'secret-pass');
    submit();

    expect(screen.getByText(/Escribe un email válido/)).toBeInTheDocument();
    expect(auth.login).not.toHaveBeenCalled();
  });

  test('sends the trimmed email and the password', async () => {
    const auth = authValue(null);
    renderLogin(auth);

    fill('  ana@example.com ', 'secret-pass');
    submit();

    await waitFor(() =>
      expect(auth.login).toHaveBeenCalledWith({
        email: 'ana@example.com',
        password: 'secret-pass',
      }),
    );
  });

  test('the button is disabled while the request is pending', async () => {
    const auth = authValue(null, { login: () => new Promise(() => {}) });
    renderLogin(auth);

    fill('ana@example.com', 'secret-pass');
    submit();

    const button = await screen.findByRole('button', { name: 'Entrando…' });
    expect(button).toBeDisabled();
  });

  test.each([
    [new ApiError(401), 'Email o contraseña incorrectos'],
    [new ApiError(403), 'Tu cuenta está bloqueada'],
    [new ApiError(400), 'Revisa los datos del formulario'],
    [new ApiError(500), /Algo ha fallado en el servidor/],
    [new NetworkError(), /No se pudo conectar con el servidor/],
  ])('shows the failure inline and enables the button again (%#)', async (error, message) => {
    const auth = authValue(null, { login: () => Promise.reject(error) });
    renderLogin(auth);

    fill('ana@example.com', 'wrong-pass');
    submit();

    expect(await screen.findByRole('alert')).toHaveTextContent(message);
    expect(screen.getByRole('button', { name: 'Iniciar sesión' })).toBeEnabled();
  });

  test('a logged-in user is sent to the trip list', () => {
    renderLogin(authValue(ANA));

    expect(screen.getByText('Mis viajes')).toBeInTheDocument();
  });

  test('returns to the page the visitor came from', () => {
    renderLogin(authValue(ANA), {
      pathname: '/login',
      state: { from: { pathname: '/trips/7', search: '', hash: '' } },
    });

    expect(screen.getByText('Detalle del viaje 7')).toBeInTheDocument();
  });
});
