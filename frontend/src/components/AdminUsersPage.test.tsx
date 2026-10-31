import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import AdminUsersPage from './AdminUsersPage';
import { renderAsUser, renderRoutes } from '../renderHelpers';
import { getUsers, setUserStatus, updateUser } from '../services/adminUserService';
import { ApiError } from '../services/apiError';
import { adminUserOf, ANA, pageOf } from '../testHelpers';
import type { AdminUser, User } from '../types';

vi.mock('../services/adminUserService', () => ({
  getUsers: vi.fn(),
  updateUser: vi.fn(),
  setUserStatus: vi.fn(),
}));

const ADMIN: User = { ...ANA, id: 9, name: 'Administrator', role: 'ADMIN' };
const ana = adminUserOf({ id: 5, name: 'Ana Ruiz', email: 'ana@example.com' });
const bob = adminUserOf({ id: 6, name: 'Bob', email: 'bob@example.com', status: 'BLOCKED' });

function renderPage() {
  return renderAsUser(<AdminUsersPage />, ADMIN, '/admin/users');
}

function search(text: string) {
  fireEvent.change(screen.getByLabelText('Buscar por nombre o email'), { target: { value: text } });
  fireEvent.click(screen.getByRole('button', { name: 'Buscar' }));
}

beforeEach(() => {
  vi.mocked(getUsers).mockReset();
  vi.mocked(updateUser).mockReset();
  vi.mocked(setUserStatus).mockReset();
});

describe('B7 AdminUsersPage list and search', () => {
  test('lists the users with email, registration date and a status badge', async () => {
    vi.mocked(getUsers).mockResolvedValue(pageOf([ana, bob]));

    renderPage();

    expect(await screen.findByRole('heading', { name: /Ana Ruiz/ })).toHaveTextContent('Activo');
    expect(screen.getByRole('heading', { name: /Bob/ })).toHaveTextContent('Bloqueado');
    expect(screen.getByText('ana@example.com')).toBeInTheDocument();
    expect(screen.getAllByText('Registrado el 1 de octubre de 2026')).toHaveLength(2);
    expect(getUsers).toHaveBeenCalledWith('', 0);
  });

  test('offers Bloquear for an active user and Desbloquear for a blocked one', async () => {
    vi.mocked(getUsers).mockResolvedValue(pageOf([ana, bob]));

    renderPage();

    expect(await screen.findByRole('button', { name: 'Bloquear Ana Ruiz' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Desbloquear Bob' })).toBeInTheDocument();
  });

  test('searching asks for the first page of the text and replaces the list', async () => {
    vi.mocked(getUsers).mockResolvedValueOnce(pageOf([ana, bob]));
    vi.mocked(getUsers).mockResolvedValueOnce(pageOf([bob]));
    renderPage();
    await screen.findByText('Ana Ruiz');

    search('  bob ');

    await waitFor(() => expect(screen.queryByText('Ana Ruiz')).not.toBeInTheDocument());
    expect(screen.getByRole('heading', { name: /Bob/ })).toBeInTheDocument();
    expect(getUsers).toHaveBeenLastCalledWith('bob', 0);
  });

  test('says so when nothing matches the search', async () => {
    vi.mocked(getUsers).mockResolvedValueOnce(pageOf([ana]));
    vi.mocked(getUsers).mockResolvedValueOnce(pageOf([]));
    renderPage();
    await screen.findByText('Ana Ruiz');

    search('zzz');

    expect(await screen.findByText('Ningún usuario coincide con la búsqueda.')).toBeInTheDocument();
  });

  test('invites nothing but states it when there are no users at all', async () => {
    vi.mocked(getUsers).mockResolvedValue(pageOf([]));

    renderPage();

    expect(await screen.findByText('Aún no hay usuarios registrados.')).toBeInTheDocument();
  });

  test('"Más resultados" loads the next page after the first', async () => {
    vi.mocked(getUsers).mockResolvedValueOnce(pageOf([ana], { last: false }));
    vi.mocked(getUsers).mockResolvedValueOnce(pageOf([bob], { page: 1 }));
    renderPage();
    await screen.findByText('Ana Ruiz');

    fireEvent.click(screen.getByRole('button', { name: 'Más resultados' }));

    expect(await screen.findByRole('heading', { name: /Bob/ })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /Ana Ruiz/ })).toBeInTheDocument();
    expect(getUsers).toHaveBeenLastCalledWith('', 1);
    expect(screen.queryByRole('button', { name: 'Más resultados' })).not.toBeInTheDocument();
  });

  test('hands a loading failure to the route error page', async () => {
    vi.mocked(getUsers).mockRejectedValue(new ApiError(500));

    renderRoutes(
      [{ path: '/', element: <AdminUsersPage />, errorElement: <p>Página de error</p> }],
      '/',
      ADMIN,
    );

    expect(await screen.findByText('Página de error')).toBeInTheDocument();
  });
});

describe('B7 AdminUsersPage edit', () => {
  async function openEditFor(user: AdminUser) {
    fireEvent.click(await screen.findByRole('button', { name: `Editar ${user.name}` }));
    return screen.findByRole('dialog', { name: 'Editar usuario' });
  }

  test('saving closes the modal, reloads the list and confirms the change', async () => {
    vi.mocked(getUsers).mockResolvedValueOnce(pageOf([ana]));
    vi.mocked(getUsers).mockResolvedValueOnce(pageOf([{ ...ana, name: 'Ana R' }]));
    vi.mocked(updateUser).mockResolvedValue({ ...ana, name: 'Ana R' });
    renderPage();
    const dialog = await openEditFor(ana);

    fireEvent.change(within(dialog).getByLabelText('Nombre'), { target: { value: 'Ana R' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByText('Datos de Ana R actualizados.')).toBeInTheDocument();
    expect(updateUser).toHaveBeenCalledWith(5, { name: 'Ana R', email: 'ana@example.com' });
    expect(await screen.findByRole('heading', { name: /Ana R/ })).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  test('cancelling closes the modal without calling the API', async () => {
    vi.mocked(getUsers).mockResolvedValue(pageOf([ana]));
    renderPage();
    const dialog = await openEditFor(ana);

    fireEvent.click(within(dialog).getByRole('button', { name: 'Cancelar' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(updateUser).not.toHaveBeenCalled();
  });
});

describe('B7 AdminUsersPage block and unblock', () => {
  test('blocking asks for confirmation, then reloads and confirms', async () => {
    vi.mocked(getUsers).mockResolvedValueOnce(pageOf([ana]));
    vi.mocked(getUsers).mockResolvedValueOnce(pageOf([{ ...ana, status: 'BLOCKED' }]));
    vi.mocked(setUserStatus).mockResolvedValue({ ...ana, status: 'BLOCKED' });
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Bloquear Ana Ruiz' }));
    const dialog = await screen.findByRole('dialog', { name: 'Bloquear cuenta' });
    expect(setUserStatus).not.toHaveBeenCalled();
    fireEvent.click(within(dialog).getByRole('button', { name: 'Bloquear' }));

    expect(await screen.findByText('Cuenta de Ana Ruiz bloqueada.')).toBeInTheDocument();
    expect(setUserStatus).toHaveBeenCalledWith(5, 'BLOCKED');
    expect(await screen.findByRole('heading', { name: /Ana Ruiz/ })).toHaveTextContent('Bloqueado');
  });

  test('unblocking sends ACTIVE and confirms', async () => {
    vi.mocked(getUsers).mockResolvedValueOnce(pageOf([bob]));
    vi.mocked(getUsers).mockResolvedValueOnce(pageOf([{ ...bob, status: 'ACTIVE' }]));
    vi.mocked(setUserStatus).mockResolvedValue({ ...bob, status: 'ACTIVE' });
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Desbloquear Bob' }));
    const dialog = await screen.findByRole('dialog', { name: 'Desbloquear cuenta' });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Desbloquear' }));

    expect(await screen.findByText('Cuenta de Bob desbloqueada.')).toBeInTheDocument();
    expect(setUserStatus).toHaveBeenCalledWith(6, 'ACTIVE');
  });
});
