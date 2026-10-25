import { cleanup, render, screen } from '@testing-library/react';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { vi, test, expect } from 'vitest';
import App from './App';
import { routes } from './routes';
import { getTrip, getTrips } from './services/tripService';
import { ApiError, NetworkError } from './services/apiError';
import { getCurrentUser } from './services/authService';
import { getProfile } from './services/userService';
import { AuthContext } from './context/AuthContext';
import type { User } from './types';
import { ANA, authValue, pageOf, tripOf } from './testHelpers';

vi.mock('./services/tripService', () => ({ getTrip: vi.fn(), getTrips: vi.fn() }));
vi.mock('./services/authService', () => ({ getCurrentUser: vi.fn() }));
vi.mock('./services/userService', () => ({ getProfile: vi.fn() }));

function renderAt(path: string, user: User | null = ANA) {
  render(
    <AuthContext.Provider value={authValue(user)}>
      <RouterProvider router={createMemoryRouter(routes, { initialEntries: [path] })} />
    </AuthContext.Provider>,
  );
}

test('the app opens on the home page inside the layout', async () => {
  vi.mocked(getCurrentUser).mockResolvedValue(null);

  render(<App />);

  expect(await screen.findByRole('link', { name: 'Iniciar sesión' })).toBeInTheDocument();
  expect(screen.getByRole('heading', { name: 'Inicio' })).toBeInTheDocument();
  expect(screen.getByRole('navigation')).toBeInTheDocument();
  expect(screen.getByRole('contentinfo')).toBeInTheDocument();
});

test('renders the trip list at /trips with the header and footer', async () => {
  vi.mocked(getTrips).mockResolvedValue(
    pageOf([
      tripOf({
        id: 1,
        name: 'Trip 1',
        destination: 'Lisbon',
        startDate: '2027-05-01',
        endDate: '2027-05-05',
      }),
    ]),
  );

  renderAt('/trips');

  expect(await screen.findByText('Trip 1')).toBeInTheDocument();
  expect(screen.getByRole('navigation')).toBeInTheDocument();
  expect(screen.getByRole('contentinfo')).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Mis viajes' })).toHaveAttribute('aria-current', 'page');
});

test.each([
  ['/trips/7/activities/new', 'Añadir actividad'],
  ['/trips/7/activities/3/edit', 'Editar actividad'],
  ['/trips/7/ai-generator', 'Generador IA'],
  ['/admin', 'Administración'],
  ['/admin/users/2/edit', 'Editar usuario'],
])('%s shows a placeholder page in the layout', (path, title) => {
  renderAt(path);

  expect(screen.getByRole('heading', { name: title })).toBeInTheDocument();
  expect(screen.getByRole('contentinfo')).toBeInTheDocument();
});

test('an unknown URL shows the 404 page inside the layout', () => {
  renderAt('/no-existe');

  expect(screen.getByRole('heading', { name: 'No encontramos esta página' })).toBeInTheDocument();
  expect(screen.getByRole('navigation')).toBeInTheDocument();
  expect(screen.getByRole('contentinfo')).toBeInTheDocument();
});

test('a server failure while loading trips shows the error page inside the layout', async () => {
  vi.mocked(getTrips).mockRejectedValue(new ApiError(500));

  renderAt('/trips');

  expect(
    await screen.findByRole('heading', { name: 'Algo ha fallado en el servidor' }),
  ).toBeInTheDocument();
  expect(screen.getByRole('contentinfo')).toBeInTheDocument();
});

test('having no connection while loading trips shows the connection error', async () => {
  vi.mocked(getTrips).mockRejectedValue(new NetworkError());

  renderAt('/trips');

  expect(
    await screen.findByRole('heading', { name: 'No hay conexión con el servidor' }),
  ).toBeInTheDocument();
});

test('B2 an anonymous visitor opening a private screen is sent to /login', () => {
  renderAt('/trips', null);

  expect(screen.getByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument();
  expect(screen.queryByText('Trip 1')).not.toBeInTheDocument();
});

test.each([
  ['/login', 'Iniciar sesión'],
  ['/register', 'Crear cuenta'],
])('B2 %s is public and shows its page', (path, title) => {
  renderAt(path, null);

  expect(screen.getByRole('heading', { name: title })).toBeInTheDocument();
});

test('B3 /profile shows the profile page of the logged-in user in the layout', async () => {
  vi.mocked(getProfile).mockResolvedValue(ANA);

  renderAt('/profile');

  expect(await screen.findByText('ana@example.com')).toBeInTheDocument();
  expect(screen.getByRole('heading', { name: 'Mi perfil' })).toBeInTheDocument();
  expect(getProfile).toHaveBeenCalledWith(ANA.id);
  expect(screen.getByRole('contentinfo')).toBeInTheDocument();
});

test('B3 an anonymous visitor opening /profile is sent to /login', () => {
  renderAt('/profile', null);

  expect(screen.getByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument();
  expect(getProfile).not.toHaveBeenCalled();
});

test('B4 /trips/new shows the creation form in the layout', () => {
  renderAt('/trips/new');

  expect(screen.getByRole('heading', { name: 'Nuevo viaje' })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Crear viaje' })).toBeInTheDocument();
  expect(screen.getByRole('contentinfo')).toBeInTheDocument();
});

test('B4 /trips/:id shows the trip detail and /trips/:id/edit its form', async () => {
  vi.mocked(getTrip).mockResolvedValue(tripOf({ id: 7, creatorId: ANA.id, name: 'Verano' }));

  renderAt('/trips/7');
  expect(await screen.findByRole('heading', { name: 'Verano' })).toBeInTheDocument();
  expect(getTrip).toHaveBeenCalledWith(7);
  cleanup();

  renderAt('/trips/7/edit');
  expect(await screen.findByLabelText('Nombre del viaje')).toHaveValue('Verano');
  expect(screen.getByRole('heading', { name: 'Editar viaje' })).toBeInTheDocument();
});

test("B4 another user's trip shows the no-access page inside the layout", async () => {
  vi.mocked(getTrip).mockRejectedValue(new ApiError(403));

  renderAt('/trips/7');

  expect(
    await screen.findByRole('heading', { name: 'No tienes acceso a esta página' }),
  ).toBeInTheDocument();
  expect(screen.getByRole('contentinfo')).toBeInTheDocument();
});

test('B4 a trip that does not exist shows the 404 page', async () => {
  vi.mocked(getTrip).mockRejectedValue(new ApiError(404));

  renderAt('/trips/999');

  expect(
    await screen.findByRole('heading', { name: 'No encontramos esta página' }),
  ).toBeInTheDocument();
});

test('B4 anonymous visitors are sent to /login from every trip screen without any request', () => {
  vi.mocked(getTrips).mockClear();
  vi.mocked(getTrip).mockClear();
  for (const path of ['/trips/new', '/trips/7', '/trips/7/edit']) {
    renderAt(path, null);
    expect(screen.getByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument();
    cleanup();
  }

  expect(getTrips).not.toHaveBeenCalled();
  expect(getTrip).not.toHaveBeenCalled();
});

test('B4 the public home page for an anonymous visitor never asks for trips', async () => {
  vi.mocked(getTrips).mockClear();
  vi.mocked(getCurrentUser).mockResolvedValue(null);
  const fetchSpy = vi.spyOn(globalThis, 'fetch');

  render(<App />);

  expect(await screen.findByRole('heading', { name: 'Inicio' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Iniciar sesión' })).toBeInTheDocument();
  expect(getTrips).not.toHaveBeenCalled();
  expect(fetchSpy.mock.calls.filter(([url]) => String(url).includes('/api/v1/trips'))).toHaveLength(
    0,
  );
  fetchSpy.mockRestore();
});
