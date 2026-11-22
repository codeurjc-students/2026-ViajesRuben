import { fireEvent, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { renderAsUser } from '../renderHelpers';
import { ApiError, NetworkError } from '../services/apiError';
import { getMyInvitations, respondToInvitation } from '../services/invitationService';
import { pageOf, receivedInvitationOf } from '../testHelpers';
import type { ReceivedInvitation } from '../types';
import PendingInvitations from './PendingInvitations';

vi.mock('../services/invitationService', () => ({
  getMyInvitations: vi.fn(),
  respondToInvitation: vi.fn(),
}));

const rome = receivedInvitationOf({ id: 1, tripName: 'Verano en Roma', inviterName: 'Ana' });
const lisbon = receivedInvitationOf({
  id: 2,
  tripId: 6,
  tripName: 'Escapada a Lisboa',
  inviterName: 'Bruno',
  sentAt: '2026-11-17T09:00:00Z',
});

beforeEach(() => {
  vi.mocked(getMyInvitations).mockReset().mockResolvedValue(pageOf([]));
  vi.mocked(respondToInvitation).mockReset();
});

function renderSection(onAccepted = vi.fn()) {
  renderAsUser(<PendingInvitations onAccepted={onAccepted} />);
  return onAccepted;
}

describe('I2 PendingInvitations', () => {
  test('shows nothing at all when there are no pending invitations', async () => {
    renderSection();

    await waitFor(() => expect(getMyInvitations).toHaveBeenCalledWith(0));
    expect(
      screen.queryByRole('heading', { name: 'Invitaciones pendientes' }),
    ).not.toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  test('lists each invitation with who invites, the trip, the date and both actions', async () => {
    vi.mocked(getMyInvitations).mockResolvedValue(pageOf([rome, lisbon]));

    renderSection();

    expect(
      await screen.findByRole('heading', { name: 'Invitaciones pendientes' }),
    ).toBeInTheDocument();
    const items = screen.getAllByRole('listitem');
    expect(items).toHaveLength(2);
    expect(items[0]).toHaveTextContent('Ana te invita a «Verano en Roma»');
    expect(items[0]).toHaveTextContent('18 nov 2026');
    expect(items[1]).toHaveTextContent('Bruno te invita a «Escapada a Lisboa»');
    expect(
      screen.getByRole('button', { name: 'Aceptar invitación a Verano en Roma' }),
    ).toBeEnabled();
    expect(
      screen.getByRole('button', { name: 'Rechazar invitación a Escapada a Lisboa' }),
    ).toBeEnabled();
  });

  test('accepting removes the row, says so and tells the list to reload the trips', async () => {
    vi.mocked(getMyInvitations).mockResolvedValue(pageOf([rome, lisbon]));
    vi.mocked(respondToInvitation).mockResolvedValue({ ...rome, status: 'ACCEPTED' });
    const onAccepted = renderSection();

    fireEvent.click(
      await screen.findByRole('button', { name: 'Aceptar invitación a Verano en Roma' }),
    );

    expect(await screen.findByRole('status')).toHaveTextContent('Te has unido a «Verano en Roma».');
    expect(respondToInvitation).toHaveBeenCalledWith(1, 'ACCEPTED');
    expect(onAccepted).toHaveBeenCalledTimes(1);
    expect(screen.getAllByRole('listitem')).toHaveLength(1);
    expect(screen.queryByText(/Verano en Roma»$/)).not.toBeInTheDocument();
    expect(getMyInvitations).toHaveBeenCalledTimes(1);
  });

  test('rejecting removes the row without telling the list to reload', async () => {
    vi.mocked(getMyInvitations).mockResolvedValue(pageOf([rome, lisbon]));
    vi.mocked(respondToInvitation).mockResolvedValue({ ...lisbon, status: 'REJECTED' });
    const onAccepted = renderSection();

    fireEvent.click(
      await screen.findByRole('button', { name: 'Rechazar invitación a Escapada a Lisboa' }),
    );

    await waitFor(() => expect(screen.getAllByRole('listitem')).toHaveLength(1));
    expect(respondToInvitation).toHaveBeenCalledWith(2, 'REJECTED');
    expect(onAccepted).not.toHaveBeenCalled();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  test('the section disappears when the last invitation is answered', async () => {
    vi.mocked(getMyInvitations).mockResolvedValue(pageOf([rome]));
    vi.mocked(respondToInvitation).mockResolvedValue({ ...rome, status: 'REJECTED' });
    renderSection();

    fireEvent.click(
      await screen.findByRole('button', { name: 'Rechazar invitación a Verano en Roma' }),
    );

    await waitFor(() =>
      expect(
        screen.queryByRole('heading', { name: 'Invitaciones pendientes' }),
      ).not.toBeInTheDocument(),
    );
  });

  test('disables every button while an answer is being sent', async () => {
    vi.mocked(getMyInvitations).mockResolvedValue(pageOf([rome, lisbon]));
    vi.mocked(respondToInvitation).mockReturnValue(new Promise<ReceivedInvitation>(() => {}));
    renderSection();

    fireEvent.click(
      await screen.findByRole('button', { name: 'Aceptar invitación a Verano en Roma' }),
    );

    for (const button of screen.getAllByRole('button')) {
      expect(button).toBeDisabled();
    }
  });

  test('a failure to answer says so, keeps the row and lets the user try again', async () => {
    vi.mocked(getMyInvitations).mockResolvedValue(pageOf([rome]));
    vi.mocked(respondToInvitation).mockRejectedValue(new NetworkError());
    const onAccepted = renderSection();

    fireEvent.click(
      await screen.findByRole('button', { name: 'Aceptar invitación a Verano en Roma' }),
    );

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudo responder a la invitación. Inténtalo de nuevo.',
    );
    expect(screen.getAllByRole('listitem')).toHaveLength(1);
    expect(
      screen.getByRole('button', { name: 'Aceptar invitación a Verano en Roma' }),
    ).toBeEnabled();
    expect(onAccepted).not.toHaveBeenCalled();
  });

  test('a 409 means it was answered elsewhere: the row leaves the list without an error', async () => {
    vi.mocked(getMyInvitations).mockResolvedValue(pageOf([rome, lisbon]));
    vi.mocked(respondToInvitation).mockRejectedValue(new ApiError(409));
    const onAccepted = renderSection();

    fireEvent.click(
      await screen.findByRole('button', { name: 'Aceptar invitación a Verano en Roma' }),
    );

    await waitFor(() => expect(screen.getAllByRole('listitem')).toHaveLength(1));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(onAccepted).not.toHaveBeenCalled();
  });

  test('loads the next page with "Más resultados" and hides the button on the last one', async () => {
    const first = Array.from({ length: 10 }, (_, i) =>
      receivedInvitationOf({ id: i + 1, tripName: `Trip ${i + 1}` }),
    );
    vi.mocked(getMyInvitations)
      .mockResolvedValueOnce(pageOf(first, { totalElements: 11, last: false }))
      .mockResolvedValueOnce(
        pageOf([receivedInvitationOf({ id: 11, tripName: 'Trip 11' })], {
          page: 1,
          totalElements: 11,
          last: true,
        }),
      );
    renderSection();

    fireEvent.click(await screen.findByRole('button', { name: 'Más resultados' }));

    expect(
      await screen.findByRole('button', { name: 'Aceptar invitación a Trip 11' }),
    ).toBeInTheDocument();
    expect(getMyInvitations).toHaveBeenLastCalledWith(1);
    expect(screen.getAllByRole('listitem')).toHaveLength(11);
    expect(screen.queryByRole('button', { name: 'Más resultados' })).not.toBeInTheDocument();
  });

  test('a failure to load them warns without hiding anything else', async () => {
    vi.mocked(getMyInvitations).mockRejectedValue(new NetworkError());

    renderSection();

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudieron cargar tus invitaciones pendientes.',
    );
  });
});
