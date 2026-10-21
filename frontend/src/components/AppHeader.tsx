import Container from 'react-bootstrap/Container';
import Nav from 'react-bootstrap/Nav';
import Navbar from 'react-bootstrap/Navbar';
import './AppHeader.css';

interface AppHeaderProps {
  userName: string;
}

export default function AppHeader({ userName }: AppHeaderProps) {
  return (
    <Navbar expand="md" className="app-header">
      <Container>
        <Navbar.Brand href="#">TripMate</Navbar.Brand>
        <Navbar.Toggle aria-controls="app-header-menu" aria-label="Abrir menú" />
        <Navbar.Collapse id="app-header-menu">
          <Nav className="me-auto">
            <Nav.Link href="#" active aria-current="page">
              Mis viajes
            </Nav.Link>
            <Nav.Link href="#">Crear viaje</Nav.Link>
            <Nav.Link href="#">Generador IA</Nav.Link>
          </Nav>
          <div className="app-header__user">
            <span className="app-header__avatar" aria-hidden="true">
              {userName.charAt(0).toUpperCase()}
            </span>
            <span>{userName}</span>
          </div>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
}
