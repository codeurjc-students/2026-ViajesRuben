import { act, fireEvent, screen } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { renderAsUser, renderRoutes } from '../renderHelpers';
import { ApiError, NetworkError } from '../services/apiError';
import { createInvitation, getInvitations } from '../services/invitationService';
import { invitationOf, pageOf } from '../testHelpers';
import type { Invitation } from '../types';
import InvitationsSection from './InvitationsSection';

vi.mock('../services/invitationService', () => ({
  getInvitations: vi.fn(),
  createInvitation: vi.fn(),
}));

beforeEach(() => {
  vi.mocked(getInvitations).mockReset().mockResolvedValue(pageOf([]));
  vi.mocked(createInvitation).mockReset();
});

function renderSection() {
  return renderAsUser(<InvitationsSection tripId={5} />);
}

function typeEmail(value: string) {
  fireEvent.change(screen.getByLabelText('Invitar por email'), { target: { value } });
}

function send() {
  fireEvent.click(screen.getByRole('button', { name: 'Enviar invitación' }));
}

describe('I1 InvitationsSection list', () => {
  test('shows a loading text and then every invitation with its date and pending state', async () => {
    vi.mocked(getInvitations).mockResolvedValue(
      pageOf([
        invitationOf({ id: 2, invitedEmail: 'b@example.com', sentAt: '2026-11-18T10:00:00Z' }),
        invitationOf({ id: 1, invitedEmail: 'a@example.com', sentAt: '2026-11-17T09:00:00Z' }),
      ]),
    );
    renderSection();

    expect(screen.getByRole('status')).toHaveTextContent('Cargando invitaciones…');
    expect(await screen.findByText('b@example.com')).toBeInTheDocument();
    expect(screen.getByText('a@example.com')).toBeInTheDocument();
    expect(screen.getAllByText('Pendiente')).toHaveLength(2);
    expect(screen.getByText('18 nov 2026')).toBeInTheDocument();
    expect(getInvitations).toHaveBeenCalledWith(5, 0);
  });

  test('I2 shows each state of an invitation with its own badge', async () => {
    vi.mocked(getInvitations).mockResolvedValue(
      pageOf([
        invitationOf({ id: 3, invitedEmail: 'c@example.com', status: 'REJECTED' }),
        invitationOf({ id: 2, invitedEmail: 'b@example.com', status: 'ACCEPTED' }),
        invitationOf({ id: 1, invitedEmail: 'a@example.com', status: 'PENDING' }),
      ]),
    );
    renderSection();

    await screen.findByText('a@example.com');

    expect(screen.getByText('c@example.com').closest('li')).toHaveTextContent('Rechazada');
    expect(screen.getByText('b@example.com').closest('li')).toHaveTextContent('Aceptada');
    expect(screen.getByText('a@example.com').closest('li')).toHaveTextContent('Pendiente');
    expect(screen.getByText('Aceptada')).toHaveClass('bg-success');
    expect(screen.getByText('Rechazada')).toHaveClass('bg-secondary');
    expect(screen.getByText('Pendiente')).toHaveClass('bg-warning');
  });

  test('invites the creator to send the first one when there are none', async () => {
    renderSection();

    expect(
      await screen.findByText('Aún no has invitado a nadie. Invita al primero.'),
    ).toBeVisible();
    expect(screen.queryByRole('list')).not.toBeInTheDocument();
  });

  test('loads the next page with "Más resultados" and hides it on the last one', async () => {
    const first: Invitation[] = Array.from({ length: 10 }, (_, i) =>
      invitationOf({ id: i + 1, invitedEmail: `friend${i + 1}@example.com` }),
    );
    vi.mocked(getInvitations)
      .mockResolvedValueOnce(pageOf(first, { totalElements: 11, last: false }))
      .mockResolvedValueOnce(
        pageOf([invitationOf({ id: 11, invitedEmail: 'friend11@example.com' })], {
          page: 1,
          totalElements: 11,
          last: true,
        }),
      );
    renderSection();

    fireEvent.click(await screen.findByRole('button', { name: 'Más resultados' }));

    expect(await screen.findByText('friend11@example.com')).toBeInTheDocument();
    expect(getInvitations).toHaveBeenLastCalledWith(5, 1);
    expect(screen.getAllByRole('listitem')).toHaveLength(11);
    expect(screen.queryByRole('button', { name: 'Más resultados' })).not.toBeInTheDocument();
  });

  test('shows the error page when the list cannot be loaded', async () => {
    vi.mocked(getInvitations).mockRejectedValue(new ApiError(500));

    renderRoutes(
      [
        {
          path: '/',
          element: <InvitationsSection tripId={5} />,
          errorElement: <p>Página de error</p>,
        },
      ],
      '/',
    );

    expect(await screen.findByText('Página de error')).toBeInTheDocument();
  });
});

describe('I1 InvitationsSection form', () => {
  test.each([
    ['', 'Escribe el email de la persona a la que quieres invitar.'],
    ['not-an-email', 'Escribe un email válido, por ejemplo nombre@correo.com.'],
    [' Ana@Example.com ', 'Revisa el email: no puede ser el tuyo.'],
  ])('does not send "%s" and says why', async (value, message) => {
    renderSection();
    await screen.findByText(/Aún no has invitado/);

    typeEmail(value);
    send();

    expect(await screen.findByText(message)).toBeInTheDocument();
    expect(createInvitation).not.toHaveBeenCalled();
  });

  test('sends the trimmed email, disables the button meanwhile and then clears the form and reloads', async () => {
    let finish: (invitation: Invitation) => void = () => undefined;
    vi.mocked(createInvitation).mockReturnValue(
      new Promise<Invitation>((resolve) => {
        finish = resolve;
      }),
    );
    renderSection();
    await screen.findByText(/Aún no has invitado/);

    typeEmail('  friend@example.com ');
    send();

    const sending = await screen.findByRole('button', { name: 'Enviando…' });
    expect(sending).toBeDisabled();
    expect(createInvitation).toHaveBeenCalledWith(5, 'friend@example.com');

    vi.mocked(getInvitations).mockResolvedValue(pageOf([invitationOf()]));
    await act(async () => finish(invitationOf()));

    expect(await screen.findByText('Invitación enviada a friend@example.com.')).toBeVisible();
    expect(screen.getByLabelText('Invitar por email')).toHaveValue('');
    expect(screen.getByRole('button', { name: 'Enviar invitación' })).toBeEnabled();
    expect(await screen.findByText('Pendiente')).toBeInTheDocument();
    expect(getInvitations).toHaveBeenCalledTimes(2);
  });

  test.each([
    [new ApiError(409), 'Ya hay una invitación pendiente para ese email.'],
    [new ApiError(400), 'Revisa el email.'],
    [new NetworkError(), 'No se pudo conectar con el servidor. Inténtalo de nuevo.'],
    [new ApiError(500), 'No se pudo enviar la invitación. Inténtalo de nuevo.'],
  ])('shows %s as an inline message and keeps the email to fix it', async (failure, message) => {
    vi.mocked(createInvitation).mockRejectedValue(failure);
    renderSection();
    await screen.findByText(/Aún no has invitado/);

    typeEmail('friend@example.com');
    send();

    expect(await screen.findByRole('alert')).toHaveTextContent(message);
    expect(screen.getByLabelText('Invitar por email')).toHaveValue('friend@example.com');
    expect(screen.getByRole('button', { name: 'Enviar invitación' })).toBeEnabled();
    expect(screen.queryByText(/Invitación enviada/)).not.toBeInTheDocument();
  });

  test('a new attempt clears the previous error and notice', async () => {
    vi.mocked(createInvitation)
      .mockRejectedValueOnce(new ApiError(409))
      .mockResolvedValueOnce(invitationOf());
    renderSection();
    await screen.findByText(/Aún no has invitado/);

    typeEmail('friend@example.com');
    send();
    await screen.findByRole('alert');
    send();

    await screen.findByText('Invitación enviada a friend@example.com.');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
