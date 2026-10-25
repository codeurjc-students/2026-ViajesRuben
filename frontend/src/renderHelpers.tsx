import { render } from '@testing-library/react';
import type { ReactElement } from 'react';
import { createMemoryRouter, MemoryRouter, type RouteObject } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { AuthContext } from './context/AuthContext';
import { ANA, authValue } from './testHelpers';
import type { User } from './types';

/** Renders a component that uses links, navigation or the logged-in user, as Ana unless told otherwise. */
export function renderAsUser(ui: ReactElement, user: User | null = ANA, path = '/') {
  return render(
    <AuthContext.Provider value={authValue(user)}>
      <MemoryRouter initialEntries={[path]}>{ui}</MemoryRouter>
    </AuthContext.Provider>,
  );
}

/** Renders routes at a path, so a page can read its params and navigate to the others. */
export function renderRoutes(
  routes: RouteObject[],
  path: string,
  user: User | null = ANA,
  state?: unknown,
) {
  const router = createMemoryRouter(routes, { initialEntries: [{ pathname: path, state }] });
  render(
    <AuthContext.Provider value={authValue(user)}>
      <RouterProvider router={router} />
    </AuthContext.Provider>,
  );
  return router;
}
