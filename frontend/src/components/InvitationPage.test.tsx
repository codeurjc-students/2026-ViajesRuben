import { act, fireEvent, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { renderRoutes } from '../renderHelpers';
import { ApiError, NetworkError } from '../services/apiError';
import { getInvitationByToken, respondToInvitation } from '../services/invitationService';
import { receivedInvitationOf } from '../testHelpers';
import type { ReceivedInvitation } from '../types';
import InvitationPage from './InvitationPage';

vi.mock('../services/invitationService', () => ({
  getInvitationByToken: vi.fn(),
  respondToInvitation: vi.fn(),
}));

beforeEach(() => {
  vi.mocked(getInvitationByToken).mockReset();
  vi.mocked(respondToInvitation).mockReset();
});

function renderPage(token = 'abc') {
  return renderRoutes(
    [
      { path: '/invitations/:token', element: <InvitationPage /> },
      { path: '/trips', element: <p>Mis viajes</p> },
      { path: '/trips/:tripId', element: <p>Detalle del viaje</p> },
    ],
    `/invitations/${token}`,
  );
}

describe('I2 InvitationPage', () => {
  test('shows who invites to which trip with Aceptar and Rechazar', async () => {
    vi.mocked(getInvitationByToken).mockResolvedValue(
      receivedInvitationOf({ inviterName: 'Ana', tripName: 'Verano en Roma' }),
    );

    renderPage('abc');

    expect(screen.getByRole('status')).toHaveTextContent('Cargando invitación…');
    expect(await screen.findByText(/Ana te invita a unirte a «Verano en Roma»/)).toBeVisible();
    expect(screen.getByRole('button', { name: 'Aceptar' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Rechazar' })).toBeEnabled();
    expect(getInvitationByToken).toHaveBeenCalledWith('abc');
  });

  test('asks browsers not to send the token on as a referrer', async () => {
    vi.mocked(getInvitationByToken).mockResolvedValue(receivedInvitationOf());

    renderPage();
    await screen.findByRole('button', { name: 'Aceptar' });

    expect(document.head.querySelector('meta[name="referrer"]')).toHaveAttribute(
      'content',
      'no-referrer',
    );
  });

  test('accepting goes to the trip with a notice', async () => {
    vi.mocked(getInvitationByToken).mockResolvedValue(receivedInvitationOf({ id: 7 }));
    vi.mocked(respondToInvitation).mockResolvedValue(
      receivedInvitationOf({ id: 7, tripId: 5, status: 'ACCEPTED' }),
    );
    const router = renderPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Aceptar' }));

    expect(await screen.findByText('Detalle del viaje')).toBeInTheDocument();
    expect(respondToInvitation).toHaveBeenCalledWith(7, 'ACCEPTED');
    expect(router.state.location.pathname).toBe('/trips/5');
    expect(router.state.location.state).toEqual({ notice: 'Te has unido a «Verano en Roma».' });
  });

  test('rejecting says so and offers My trips', async () => {
    vi.mocked(getInvitationByToken).mockResolvedValue(receivedInvitationOf({ id: 7 }));
    vi.mocked(respondToInvitation).mockResolvedValue(
      receivedInvitationOf({ id: 7, status: 'REJECTED' }),
    );
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Rechazar' }));

    expect(
      await screen.findByText('Has rechazado la invitación a «Verano en Roma».'),
    ).toBeVisible();
    expect(respondToInvitation).toHaveBeenCalledWith(7, 'REJECTED');
    expect(screen.getByRole('link', { name: 'Ir a Mis viajes' })).toHaveAttribute('href', '/trips');
    expect(screen.queryByRole('button', { name: 'Aceptar' })).not.toBeInTheDocument();
  });

  test('disables both buttons while the answer is being sent', async () => {
    vi.mocked(getInvitationByToken).mockResolvedValue(receivedInvitationOf());
    let finish: (value: ReceivedInvitation) => void = () => {};
    vi.mocked(respondToInvitation).mockReturnValue(
      new Promise((resolve) => {
        finish = resolve;
      }),
    );
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Rechazar' }));

    expect(screen.getByRole('button', { name: 'Aceptar' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Rechazar' })).toBeDisabled();
    await act(async () => finish(receivedInvitationOf({ status: 'REJECTED' })));
  });

  test('an invitation already accepted offers to see the trip and no buttons', async () => {
    vi.mocked(getInvitationByToken).mockResolvedValue(
      receivedInvitationOf({ tripId: 9, status: 'ACCEPTED' }),
    );

    renderPage();

    expect(
      await screen.findByText('Ya has aceptado la invitación a «Verano en Roma».'),
    ).toBeVisible();
    expect(screen.getByRole('link', { name: 'Ver viaje' })).toHaveAttribute('href', '/trips/9');
    expect(screen.queryByRole('button', { name: 'Aceptar' })).not.toBeInTheDocument();
  });

  test('an invitation already rejected says so and has no buttons', async () => {
    vi.mocked(getInvitationByToken).mockResolvedValue(receivedInvitationOf({ status: 'REJECTED' }));

    renderPage();

    expect(
      await screen.findByText('Has rechazado la invitación a «Verano en Roma».'),
    ).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Rechazar' })).not.toBeInTheDocument();
  });

  test('an invitation for another account says to log in with the right email', async () => {
    vi.mocked(getInvitationByToken).mockRejectedValue(new ApiError(403));

    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Esta invitación es para otra cuenta. Inicia sesión con el email al que se envió.',
    );
    expect(screen.getByRole('link', { name: 'Ir a Mis viajes' })).toBeInTheDocument();
  });

  test('an unknown invitation or a deleted trip says it does not exist', async () => {
    vi.mocked(getInvitationByToken).mockRejectedValue(new ApiError(404));

    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Esta invitación no existe o el viaje se ha eliminado.',
    );
  });

  test('a failure to load says to try again', async () => {
    vi.mocked(getInvitationByToken).mockRejectedValue(new NetworkError());

    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudo cargar la invitación. Inténtalo de nuevo.',
    );
  });

  test('a failure to answer shows an error and lets the user try again', async () => {
    vi.mocked(getInvitationByToken).mockResolvedValue(receivedInvitationOf());
    vi.mocked(respondToInvitation).mockRejectedValue(new NetworkError());
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Aceptar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudo responder a la invitación. Inténtalo de nuevo.',
    );
    expect(screen.getByRole('button', { name: 'Aceptar' })).toBeEnabled();
  });

  test('a 409 means it was answered meanwhile: the page loads it again and shows the answer', async () => {
    vi.mocked(getInvitationByToken)
      .mockResolvedValueOnce(receivedInvitationOf())
      .mockResolvedValueOnce(receivedInvitationOf({ status: 'ACCEPTED' }));
    vi.mocked(respondToInvitation).mockRejectedValue(new ApiError(409));
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Rechazar' }));

    expect(
      await screen.findByText('Ya has aceptado la invitación a «Verano en Roma».'),
    ).toBeVisible();
    await waitFor(() => expect(getInvitationByToken).toHaveBeenCalledTimes(2));
  });
});
