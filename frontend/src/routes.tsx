import type { RouteObject } from 'react-router';
import AppLayout from './components/AppLayout';
import ErrorPage from './components/ErrorPage';
import LoginPage from './components/LoginPage';
import PlaceholderPage from './components/PlaceholderPage';
import ProfilePage from './components/ProfilePage';
import RegisterPage from './components/RegisterPage';
import RequireAuth from './components/RequireAuth';
import TripDetailPage from './components/TripDetailPage';
import TripFormPage from './components/TripFormPage';
import TripList from './components/TripList';

/** One route per screen in docs/analysis.md; trips, profile and auth are built so far. */
export const routes: RouteObject[] = [
  {
    element: <AppLayout />,
    children: [
      {
        errorElement: <ErrorPage />,
        children: [
          { path: '/', element: <PlaceholderPage title="Inicio" /> },
          { path: '/login', element: <LoginPage /> },
          { path: '/register', element: <RegisterPage /> },
          {
            // Every screen below needs a logged-in user.
            element: <RequireAuth />,
            children: [
              { path: '/trips', element: <TripList /> },
              { path: '/trips/new', element: <TripFormPage /> },
              { path: '/trips/:tripId', element: <TripDetailPage /> },
              { path: '/trips/:tripId/edit', element: <TripFormPage /> },
              {
                path: '/trips/:tripId/activities/new',
                element: <PlaceholderPage title="Añadir actividad" />,
              },
              {
                path: '/trips/:tripId/activities/:activityId/edit',
                element: <PlaceholderPage title="Editar actividad" />,
              },
              {
                path: '/trips/:tripId/ai-generator',
                element: <PlaceholderPage title="Generador IA" />,
              },
              { path: '/profile', element: <ProfilePage /> },
              { path: '/admin', element: <PlaceholderPage title="Administración" /> },
              {
                path: '/admin/users/:userId/edit',
                element: <PlaceholderPage title="Editar usuario" />,
              },
            ],
          },
          { path: '*', element: <ErrorPage notFound /> },
        ],
      },
    ],
  },
];
