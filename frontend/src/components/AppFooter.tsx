import Container from 'react-bootstrap/Container';
import './AppFooter.css';

export default function AppFooter() {
  return (
    <footer className="app-footer">
      <Container className="app-footer__content">
        <span className="app-footer__brand">TripMate</span>
        <span>Proyecto de fin de grado, ETSII-URJC</span>
        <span>{`© ${new Date().getFullYear()} TripMate`}</span>
      </Container>
    </footer>
  );
}
