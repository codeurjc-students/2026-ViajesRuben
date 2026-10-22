import { render, screen } from '@testing-library/react';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { test, expect } from 'vitest';
import ErrorPage from './ErrorPage';
import { ApiError, NetworkError } from '../services/apiError';

function renderWithRouteError(error: unknown) {
  const router = createMemoryRouter(
    [
      { path: '/', element: <p>Inicio</p> },
      {
        path: '/fail',
        loader: () => {
          throw error;
        },
        element: <p>never</p>,
        errorElement: <ErrorPage />,
      },
    ],
    { initialEntries: ['/fail'] },
  );
  render(<RouterProvider router={router} />);
  return router;
}

test('shows the 404 message when told the page does not exist', () => {
  const router = createMemoryRouter([{ path: '*', element: <ErrorPage notFound /> }], {
    initialEntries: ['/lo-que-sea'],
  });
  render(<RouterProvider router={router} />);

  expect(screen.getByRole('heading', { name: 'No encontramos esta página' })).toBeInTheDocument();
});

test('shows the 404 message for a 404 from the API', async () => {
  renderWithRouteError(new ApiError(404));

  expect(
    await screen.findByRole('heading', { name: 'No encontramos esta página' }),
  ).toBeInTheDocument();
});

test('shows the server message for a 5xx', async () => {
  renderWithRouteError(new ApiError(503));

  expect(
    await screen.findByRole('heading', { name: 'Algo ha fallado en el servidor' }),
  ).toBeInTheDocument();
});

test('shows the connection message when there is no connection', async () => {
  renderWithRouteError(new NetworkError());

  expect(
    await screen.findByRole('heading', { name: 'No hay conexión con el servidor' }),
  ).toBeInTheDocument();
});

test('shows the server message for any unexpected error', async () => {
  renderWithRouteError(new Error('boom'));

  expect(
    await screen.findByRole('heading', { name: 'Algo ha fallado en el servidor' }),
  ).toBeInTheDocument();
});

test('the button goes back to the home page', async () => {
  const router = renderWithRouteError(new ApiError(500));

  (await screen.findByRole('link', { name: 'Volver al inicio' })).click();

  expect(await screen.findByText('Inicio')).toBeInTheDocument();
  expect(router.state.location.pathname).toBe('/');
});
