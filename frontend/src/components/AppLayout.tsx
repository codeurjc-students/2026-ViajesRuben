import { Outlet } from 'react-router';
import AppFooter from './AppFooter';
import AppHeader from './AppHeader';

export default function AppLayout() {
  return (
    <div className="d-flex flex-column min-vh-100">
      <AppHeader />
      <Outlet />
      <AppFooter />
    </div>
  );
}
