import { Outlet } from 'react-router';
import { ApiError } from '../services/apiError';
import { useAuth } from '../hooks/useAuth';

/**
 * Layout route inside RequireAuth: shows its child routes to the administrator only. Anyone else
 * gets the 403 error page, thrown so the route's error element renders it inside the layout.
 */
export default function RequireAdmin() {
  const { user } = useAuth();

  if (user?.role !== 'ADMIN') throw new ApiError(403);
  return <Outlet />;
}
