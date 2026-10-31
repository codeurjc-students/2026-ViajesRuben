import { useCallback, useState, type SubmitEvent } from 'react';
import Alert from 'react-bootstrap/Alert';
import Badge from 'react-bootstrap/Badge';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Container from 'react-bootstrap/Container';
import Form from 'react-bootstrap/Form';
import Spinner from 'react-bootstrap/Spinner';
import { usePagedList } from '../hooks/usePagedList';
import { getUsers } from '../services/adminUserService';
import type { AdminUser } from '../types';
import { formatMemberSince } from '../utils/profileForm';
import BlockUserModal from './BlockUserModal';
import EditUserModal from './EditUserModal';
import LoadMoreButton from './LoadMoreButton';
import './AdminUsersPage.css';

interface AdminUserListProps {
  search: string;
}

/** The users matching `search`; mounted again (key) for every new search so it starts at page 0. */
function AdminUserList({ search }: Readonly<AdminUserListProps>) {
  const fetchPage = useCallback((page: number) => getUsers(search, page), [search]);
  const { items: users, loaded, loading, last, error, loadMore, reload } = usePagedList(fetchPage);
  const [editing, setEditing] = useState<AdminUser>();
  const [blocking, setBlocking] = useState<AdminUser>();
  const [notice, setNotice] = useState<string>();

  // Rethrown so the route's error page (inside the layout) is shown.
  if (error) throw error;

  function edited(user: AdminUser) {
    setEditing(undefined);
    setNotice(`Datos de ${user.name} actualizados.`);
    reload();
  }

  function statusChanged(user: AdminUser) {
    setBlocking(undefined);
    setNotice(
      user.status === 'BLOCKED'
        ? `Cuenta de ${user.name} bloqueada.`
        : `Cuenta de ${user.name} desbloqueada.`,
    );
    reload();
  }

  return (
    <>
      {notice && (
        <Alert variant="success" role="status" dismissible onClose={() => setNotice(undefined)}>
          {notice}
        </Alert>
      )}
      {!loaded && (
        <div className="text-center text-white py-4">
          <Spinner animation="border" role="status">
            <span className="visually-hidden">Cargando…</span>
          </Spinner>
        </div>
      )}
      {loaded && users.length === 0 && (
        <Card className="admin-users__empty">
          <p className="mb-0">
            {search
              ? 'Ningún usuario coincide con la búsqueda.'
              : 'Aún no hay usuarios registrados.'}
          </p>
        </Card>
      )}
      {users.length > 0 && (
        <ul className="admin-users__list">
          {users.map((user) => (
            <li key={user.id}>
              <Card className="admin-user">
                <div className="admin-user__info">
                  <h2 className="admin-user__name">
                    {user.name}
                    <Badge bg={user.status === 'ACTIVE' ? 'success' : 'danger'} className="ms-2">
                      {user.status === 'ACTIVE' ? 'Activo' : 'Bloqueado'}
                    </Badge>
                  </h2>
                  <p className="admin-user__meta">{user.email}</p>
                  <p className="admin-user__meta">
                    {`Registrado el ${formatMemberSince(user.registrationDate)}`}
                  </p>
                </div>
                <div className="admin-user__actions">
                  <Button
                    variant="outline-primary"
                    size="sm"
                    aria-label={`Editar ${user.name}`}
                    onClick={() => setEditing(user)}
                  >
                    Editar
                  </Button>
                  <Button
                    variant={user.status === 'ACTIVE' ? 'outline-danger' : 'outline-secondary'}
                    size="sm"
                    aria-label={`${user.status === 'ACTIVE' ? 'Bloquear' : 'Desbloquear'} ${user.name}`}
                    onClick={() => setBlocking(user)}
                  >
                    {user.status === 'ACTIVE' ? 'Bloquear' : 'Desbloquear'}
                  </Button>
                </div>
              </Card>
            </li>
          ))}
        </ul>
      )}
      {loaded && <LoadMoreButton last={last} loading={loading} onClick={loadMore} />}
      {editing && (
        <EditUserModal user={editing} onCancel={() => setEditing(undefined)} onSaved={edited} />
      )}
      {blocking && (
        <BlockUserModal
          user={blocking}
          onCancel={() => setBlocking(undefined)}
          onChanged={statusChanged}
        />
      )}
    </>
  );
}

/** User management for the administrator: search, list, edit and block or unblock. */
export default function AdminUsersPage() {
  const [text, setText] = useState('');
  const [search, setSearch] = useState('');

  function handleSearch(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setSearch(text.trim());
  }

  return (
    <Container as="main" className="app-main py-4">
      <h1 className="admin-users__title text-white mb-4">Usuarios</h1>
      <Card className="admin-users__search">
        <Form role="search" onSubmit={handleSearch}>
          <Form.Label htmlFor="admin-users-search">Buscar por nombre o email</Form.Label>
          <div className="admin-users__search-row">
            <Form.Control
              id="admin-users-search"
              type="search"
              value={text}
              onChange={(event) => setText(event.target.value)}
            />
            <Button type="submit">Buscar</Button>
          </div>
        </Form>
      </Card>
      <AdminUserList key={search} search={search} />
    </Container>
  );
}
