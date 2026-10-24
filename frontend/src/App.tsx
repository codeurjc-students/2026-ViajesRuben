import { createBrowserRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import AuthProvider from './components/AuthProvider';
import { routes } from './routes';

const router = createBrowserRouter(routes);

export default function App() {
  return (
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>
  );
}
