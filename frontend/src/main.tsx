import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import '@fontsource/overpass/400.css';
import '@fontsource/overpass/600.css';
import '@fontsource/overpass/800.css';
import '@fontsource/overpass-mono/400.css';
import 'bootstrap/dist/css/bootstrap.min.css';
import './styles/theme.css';
import './styles/bootstrap-theme.css';
import App from './App';

createRoot(document.getElementById('root') as HTMLElement).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
