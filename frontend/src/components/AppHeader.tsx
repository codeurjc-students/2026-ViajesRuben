import { useState } from 'react';
import Button from 'react-bootstrap/Button';
import Container from 'react-bootstrap/Container';
import Nav from 'react-bootstrap/Nav';
import Navbar from 'react-bootstrap/Navbar';
import { Link, NavLink, useNavigate } from 'react-router';
import { useAuth } from '../hooks/useAuth';
import UserAvatar from './UserAvatar';
import './AppHeader.css';

export default function AppHeader() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [loggingOut, setLoggingOut] = useState(false);

  async function handleLogout() {
    setLoggingOut(true);
    try {
      await logout();
      navigate('/', { replace: true });
    } finally {
      setLoggingOut(false);
    }
  }

  return (
    <Navbar expand="md" className="app-header">
      <Container>
        <Navbar.Brand as={Link} to="/">
          <img
            src="/App_logo_without_background.png"
            alt="TripMate"
            width="1024"
            height="254"
            className="app-header__logo"
          />
        </Navbar.Brand>
        <Navbar.Toggle aria-controls="app-header-menu" aria-label="Abrir menú" />
        <Navbar.Collapse id="app-header-menu">
          {user ? (
            <>
              <Nav className="me-auto">
                <Nav.Link as={NavLink} to="/trips" end>
                  Mis viajes
                </Nav.Link>
                <Nav.Link as={NavLink} to="/trips/new">
                  Crear viaje
                </Nav.Link>
                <Nav.Link href="#">Generador IA</Nav.Link>
              </Nav>
              <div className="app-header__user">
                <Link to="/profile" className="app-header__profile" aria-label="Mi perfil">
                  <UserAvatar user={user} />
                  <span>{user.name}</span>
                </Link>
                <Button
                  variant="outline-primary"
                  size="sm"
                  disabled={loggingOut}
                  onClick={handleLogout}
                >
                  Cerrar sesión
                </Button>
              </div>
            </>
          ) : (
            <Nav className="ms-auto">
              <Nav.Link as={NavLink} to="/login">
                Iniciar sesión
              </Nav.Link>
              <Nav.Link as={NavLink} to="/register">
                Registrarse
              </Nav.Link>
            </Nav>
          )}
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
}
