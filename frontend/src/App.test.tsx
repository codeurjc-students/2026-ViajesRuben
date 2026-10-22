import { render, screen } from '@testing-library/react';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { vi, test, expect } from 'vitest';
import App from './App';
import { routes } from './routes';
import { getTrips } from './services/tripService';
import { ApiError, NetworkError } from './services/apiError';
import { pageOf } from './testHelpers';

vi.mock('./services/tripService', () => ({ getTrips: vi.fn() }));

function renderAt(path: string) {
  render(<RouterProvider router={createMemoryRouter(routes, { initialEntries: [path] })} />);
}

test('the app opens on the home page inside the layout', () => {
  render(<App />);

  expect(screen.getByRole('heading', { name: 'Inicio' })).toBeInTheDocument();
  expect(screen.getByRole('navigation')).toBeInTheDocument();
  expect(screen.getByRole('contentinfo')).toBeInTheDocument();
});

test('renders the trip list at /trips with the header and footer', async () => {
  vi.mocked(getTrips).mockResolvedValue(
    pageOf([
      {
        id: 1,
        name: 'Trip 1',
        destination: 'Lisbon',
        startDate: '2027-05-01',
        endDate: '2027-05-05',
      },
    ]),
  );

  renderAt('/trips');

  expect(await screen.findByText('Viaje a Lisbon')).toBeInTheDocument();
  expect(screen.getByRole('navigation')).toBeInTheDocument();
  expect(screen.getByRole('contentinfo')).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Mis viajes' })).toHaveAttribute('aria-current', 'page');
});

test.each([
  ['/login', 'Iniciar sesión'],
  ['/register', 'Crear cuenta'],
  ['/trips/new', 'Crear viaje'],
  ['/trips/7', 'Detalle del viaje'],
  ['/trips/7/edit', 'Editar viaje'],
  ['/trips/7/activities/new', 'Añadir actividad'],
  ['/trips/7/activities/3/edit', 'Editar actividad'],
  ['/trips/7/ai-generator', 'Generador IA'],
  ['/profile', 'Perfil'],
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
