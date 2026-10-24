import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router';
import { test, expect, describe } from 'vitest';
import RequireAuth from './RequireAuth';
import { AuthContext, type AuthContextValue } from '../context/AuthContext';
import { authValue } from '../testHelpers';

function LoginProbe() {
  const location = useLocation();
  const from = (location.state as { from?: { pathname: string } } | null)?.from?.pathname;
  return <p>Login, vuelve a {from}</p>;
}

function renderProtected(auth: AuthContextValue) {
  render(
    <AuthContext.Provider value={auth}>
      <MemoryRouter initialEntries={['/trips']}>
        <Routes>
          <Route path="/login" element={<LoginProbe />} />
          <Route element={<RequireAuth />}>
            <Route path="/trips" element={<p>Zona privada</p>} />
          </Route>
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

describe('B2 RequireAuth', () => {
  test('shows the protected page to a logged-in user', () => {
    renderProtected(authValue());

    expect(screen.getByText('Zona privada')).toBeInTheDocument();
  });

  test('sends an anonymous visitor to /login remembering the page they wanted', () => {
    renderProtected(authValue(null));

    expect(screen.getByText('Login, vuelve a /trips')).toBeInTheDocument();
    expect(screen.queryByText('Zona privada')).not.toBeInTheDocument();
  });

  test('shows a spinner while the session is still being checked', () => {
    renderProtected(authValue(null, { loading: true }));

    expect(screen.getByRole('status')).toHaveTextContent('Cargando…');
    expect(screen.queryByText('Zona privada')).not.toBeInTheDocument();
    expect(screen.queryByText(/Login/)).not.toBeInTheDocument();
  });
});
