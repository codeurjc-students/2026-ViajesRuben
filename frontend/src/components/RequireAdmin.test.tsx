import { render, screen } from '@testing-library/react';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { describe, expect, test } from 'vitest';
import ErrorPage from './ErrorPage';
import RequireAdmin from './RequireAdmin';
import { AuthContext } from '../context/AuthContext';
import { ANA, authValue } from '../testHelpers';
import type { User } from '../types';

const ADMIN: User = { ...ANA, id: 9, name: 'Administrator', role: 'ADMIN' };

function renderAdminArea(user: User) {
  const router = createMemoryRouter(
    [
      {
        errorElement: <ErrorPage />,
        children: [
          {
            element: <RequireAdmin />,
            children: [{ path: '/admin/users', element: <p>Zona de administración</p> }],
          },
        ],
      },
    ],
    { initialEntries: ['/admin/users'] },
  );
  render(
    <AuthContext.Provider value={authValue(user)}>
      <RouterProvider router={router} />
    </AuthContext.Provider>,
  );
}

describe('B7 RequireAdmin', () => {
  test('shows the admin area to the administrator', () => {
    renderAdminArea(ADMIN);

    expect(screen.getByText('Zona de administración')).toBeInTheDocument();
  });

  test('shows the 403 error page to a regular user', () => {
    renderAdminArea(ANA);

    expect(
      screen.getByRole('heading', { name: 'No tienes acceso a esta página' }),
    ).toBeInTheDocument();
    expect(screen.queryByText('Zona de administración')).not.toBeInTheDocument();
  });
});
