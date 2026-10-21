import AppFooter from './components/AppFooter';
import AppHeader from './components/AppHeader';
import TripList from './components/TripList';

// Placeholder until login (B2) provides the current user.
const CURRENT_USER_NAME = 'Carlos';

export default function App() {
  return (
    <div className="d-flex flex-column min-vh-100">
      <AppHeader userName={CURRENT_USER_NAME} />
      <TripList />
      <AppFooter />
    </div>
  );
}
