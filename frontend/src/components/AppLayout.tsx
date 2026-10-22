import { Outlet } from 'react-router';
import AppFooter from './AppFooter';
import AppHeader from './AppHeader';

// Placeholder until login (B2) provides the current user.
const CURRENT_USER_NAME = 'Carlos';

export default function AppLayout() {
  return (
    <div className="d-flex flex-column min-vh-100">
      <AppHeader userName={CURRENT_USER_NAME} />
      <Outlet />
      <AppFooter />
    </div>
  );
}
