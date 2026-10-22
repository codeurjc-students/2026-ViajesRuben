import Container from 'react-bootstrap/Container';
import './StatusPanel.css';

interface PlaceholderPageProps {
  title: string;
}

/** Stand-in for a screen that a later feature will build. */
export default function PlaceholderPage({ title }: PlaceholderPageProps) {
  return (
    <Container as="main" className="app-main py-4">
      <div className="status-panel">
        <h1 className="h2">{title}</h1>
        <p className="mb-0">Esta pantalla estará disponible próximamente.</p>
      </div>
    </Container>
  );
}
