import type { RouteObject } from 'react-router';
import ActivityFormPage from './components/ActivityFormPage';
import AdminTripsPage from './components/AdminTripsPage';
import AdminUsersPage from './components/AdminUsersPage';
import AppLayout from './components/AppLayout';
import ErrorPage from './components/ErrorPage';
import HomePage from './components/HomePage';
import LoginPage from './components/LoginPage';
import PlaceholderPage from './components/PlaceholderPage';
import ProfilePage from './components/ProfilePage';
import PublicTripPage from './components/PublicTripPage';
import RegisterPage from './components/RegisterPage';
import RequireAdmin from './components/RequireAdmin';
import RequireAuth from './components/RequireAuth';
import TripDetailPage from './components/TripDetailPage';
import TripFormPage from './components/TripFormPage';
import TripList from './components/TripList';

/** One route per screen in docs/analysis.md; the home page, trips, profile, auth, user management and trip moderation are built so far. */
export const routes: RouteObject[] = [
  {
    element: <AppLayout />,
    children: [
      {
        errorElement: <ErrorPage />,
        children: [
          { path: '/', element: <HomePage /> },
          // The public itineraries page (search, copy) arrives with I8.
          { path: '/public', element: <PlaceholderPage title="Viajes públicos" /> },
          { path: '/public/:tripId', element: <PublicTripPage /> },
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
              { path: '/trips/:tripId/activities/new', element: <ActivityFormPage /> },
              {
                path: '/trips/:tripId/activities/:activityId/edit',
                element: <ActivityFormPage />,
              },
              {
                path: '/trips/:tripId/ai-generator',
                element: <PlaceholderPage title="Generador IA" />,
              },
              { path: '/profile', element: <ProfilePage /> },
              {
                // Only the administrator; anyone else gets the 403 error page.
                element: <RequireAdmin />,
                children: [
                  { path: '/admin', element: <PlaceholderPage title="Administración" /> },
                  { path: '/admin/users', element: <AdminUsersPage /> },
                  { path: '/admin/trips', element: <AdminTripsPage /> },
                ],
              },
            ],
          },
          { path: '*', element: <ErrorPage notFound /> },
        ],
      },
    ],
  },
];
