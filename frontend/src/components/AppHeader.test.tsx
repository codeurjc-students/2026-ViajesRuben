import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
import { test, expect, describe } from 'vitest';
import AppHeader from './AppHeader';
import { AuthContext, type AuthContextValue } from '../context/AuthContext';
import { ANA, authValue } from '../testHelpers';

function renderAt(path: string, auth: AuthContextValue = authValue(ANA)) {
  render(
    <AuthContext.Provider value={auth}>
      <MemoryRouter initialEntries={[path]}>
        <AppHeader />
        <Routes>
          <Route path="*" element={<p>Pagina {path}</p>} />
          <Route path="/" element={<p>Inicio publico</p>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

test('shows the brand, the navigation links and the user name', () => {
  renderAt('/trips');

  expect(screen.getByAltText('TripMate')).toHaveAttribute(
    'src',
    '/App_logo_without_background.png',
  );
  expect(screen.getByText('Crear viaje')).toBeInTheDocument();
  expect(screen.getByText('Generador IA')).toBeInTheDocument();
  expect(screen.getByText('Ana')).toBeInTheDocument();
});

test('the brand links to the home page and the menu to the trip screens', () => {
  renderAt('/trips');

  expect(screen.getByRole('link', { name: 'TripMate' })).toHaveAttribute('href', '/');
  expect(screen.getByRole('link', { name: 'Mis viajes' })).toHaveAttribute('href', '/trips');
  expect(screen.getByRole('link', { name: 'Crear viaje' })).toHaveAttribute('href', '/trips/new');
});

test('marks My Trips as the current page on /trips', () => {
  renderAt('/trips');

  expect(screen.getByText('Mis viajes')).toHaveAttribute('aria-current', 'page');
  expect(screen.getByText('Crear viaje')).not.toHaveAttribute('aria-current');
});

test('marks Create trip as the current page on /trips/new', () => {
  renderAt('/trips/new');

  expect(screen.getByText('Crear viaje')).toHaveAttribute('aria-current', 'page');
  expect(screen.getByText('Mis viajes')).not.toHaveAttribute('aria-current');
});

describe('B2 header and the session', () => {
  test('an anonymous visitor sees the login and register links and no trip menu', () => {
    renderAt('/', authValue(null));

    expect(screen.getByRole('link', { name: 'Iniciar sesión' })).toHaveAttribute('href', '/login');
    expect(screen.getByRole('link', { name: 'Registrarse' })).toHaveAttribute('href', '/register');
    expect(screen.queryByRole('link', { name: 'Mis viajes' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Cerrar sesión' })).not.toBeInTheDocument();
  });

  test('a logged-in user sees the name and a logout button, not the login links', () => {
    renderAt('/trips');

    expect(screen.getByText('Ana')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cerrar sesión' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Iniciar sesión' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Registrarse' })).not.toBeInTheDocument();
  });

  test('logging out calls the session and goes to the home page', async () => {
    const auth = authValue(ANA);
    renderAt('/trips', auth);

    fireEvent.click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    await waitFor(() => expect(screen.getByText('Inicio publico')).toBeInTheDocument());
    expect(auth.logout).toHaveBeenCalledOnce();
  });
});
