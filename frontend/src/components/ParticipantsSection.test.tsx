import { fireEvent, screen, within } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { renderAsUser, renderRoutes } from '../renderHelpers';
import { ApiError } from '../services/apiError';
import { getParticipants, removeParticipant } from '../services/participantService';
import { ANA, pageOf, participantOf } from '../testHelpers';
import type { Participant } from '../types';
import ParticipantsSection from './ParticipantsSection';

vi.mock('../services/participantService', () => ({
  getParticipants: vi.fn(),
  removeParticipant: vi.fn(),
}));

beforeEach(() => {
  vi.mocked(getParticipants).mockReset().mockResolvedValue(pageOf([]));
  vi.mocked(removeParticipant).mockReset();
});

function renderSection(canRemove: boolean) {
  return renderAsUser(
    <ParticipantsSection tripId={5} tripName="Verano en Roma" canRemove={canRemove} />,
  );
}

const BOB = participantOf({ userId: 2, name: 'Bob', email: 'bob@example.com' });
const CARLA = participantOf({
  userId: 3,
  name: 'Carla',
  email: 'carla@example.com',
  joinedAt: '2026-11-19T09:00:00Z',
});

describe('I3 ParticipantsSection list', () => {
  test('shows a loading text and then every participant with their name, email and join date', async () => {
    vi.mocked(getParticipants).mockResolvedValue(pageOf([BOB, CARLA]));
    renderSection(true);

    expect(screen.getByRole('status')).toHaveTextContent('Cargando participantes…');
    expect(await screen.findByText('Bob')).toBeInTheDocument();
    expect(screen.getByText('bob@example.com')).toBeInTheDocument();
    expect(screen.getByText('Carla')).toBeInTheDocument();
    expect(screen.getByText('18 nov 2026')).toBeInTheDocument();
    expect(screen.getByText('19 nov 2026')).toBeInTheDocument();
    expect(screen.getAllByText(/Se unió el/)).toHaveLength(2);
    expect(getParticipants).toHaveBeenCalledWith(5, 0);
  });

  test('shows no email when the server sends none', async () => {
    vi.mocked(getParticipants).mockResolvedValue(pageOf([participantOf({ name: 'Bob' })]));
    renderSection(false);

    await screen.findByText('Bob');

    expect(screen.getByRole('listitem')).not.toHaveTextContent('@');
  });

  test('marks the logged-in user with "(tú)"', async () => {
    vi.mocked(getParticipants).mockResolvedValue(
      pageOf([participantOf({ userId: ANA.id, name: 'Ana' }), BOB]),
    );
    renderSection(false);

    await screen.findByText('Bob');

    expect(screen.getByText('Ana (tú)')).toBeInTheDocument();
    expect(screen.queryByText('Bob (tú)')).not.toBeInTheDocument();
  });

  test('shows the picture of a participant with an avatar and the initial of one without', async () => {
    vi.mocked(getParticipants).mockResolvedValue(
      pageOf([
        participantOf({ userId: 2, name: 'Bob', avatarId: '3f2c9d1e-7a54-4b8e-9c10-5d6e7f8a9b0c' }),
        participantOf({ userId: 3, name: 'Carla', avatarId: null }),
      ]),
    );
    renderSection(false);

    await screen.findByText('Bob');

    const [bob, carla] = screen.getAllByRole('listitem');
    expect(bob.querySelector('img')).toHaveAttribute(
      'src',
      expect.stringContaining('3f2c9d1e-7a54-4b8e-9c10-5d6e7f8a9b0c'),
    );
    expect(carla.querySelector('img')).toBeNull();
    expect(within(carla).getByText('C')).toBeInTheDocument();
  });

  test('invites the creator to invite someone when nobody joined yet', async () => {
    renderSection(true);

    expect(
      await screen.findByText('Aún no se ha unido nadie. Invita a alguien desde Invitaciones.'),
    ).toBeVisible();
    expect(screen.queryByRole('list')).not.toBeInTheDocument();
  });

  test('says only that nobody joined yet to a participant', async () => {
    renderSection(false);

    expect(await screen.findByText('Aún no se ha unido nadie.')).toBeVisible();
  });

  test('loads the next page with "Más resultados" and hides it on the last one', async () => {
    const first: Participant[] = Array.from({ length: 10 }, (_, i) =>
      participantOf({ userId: i + 2, name: `Friend ${i + 1}` }),
    );
    vi.mocked(getParticipants)
      .mockResolvedValueOnce(pageOf(first, { totalElements: 11, last: false }))
      .mockResolvedValueOnce(
        pageOf([participantOf({ userId: 12, name: 'Friend 11' })], {
          page: 1,
          totalElements: 11,
          last: true,
        }),
      );
    renderSection(false);

    fireEvent.click(await screen.findByRole('button', { name: 'Más resultados' }));

    expect(await screen.findByText('Friend 11')).toBeInTheDocument();
    expect(getParticipants).toHaveBeenLastCalledWith(5, 1);
    expect(screen.getAllByRole('listitem')).toHaveLength(11);
    expect(screen.queryByRole('button', { name: 'Más resultados' })).not.toBeInTheDocument();
  });

  test('shows the error page when the list cannot be loaded', async () => {
    vi.mocked(getParticipants).mockRejectedValue(new ApiError(500));

    renderRoutes(
      [
        {
          path: '/',
          element: <ParticipantsSection tripId={5} tripName="Verano en Roma" canRemove={false} />,
          errorElement: <p>Página de error</p>,
        },
      ],
      '/',
    );

    expect(await screen.findByText('Página de error')).toBeInTheDocument();
  });
});

describe('I3 ParticipantsSection leaving', () => {
  const routes = [
    {
      path: '/trips/:tripId',
      element: <ParticipantsSection tripId={5} tripName="Verano en Roma" canRemove={false} />,
    },
    { path: '/trips', element: <p>Lista de viajes</p> },
  ];

  test('offers "Salir del viaje" to a participant and not to the creator', async () => {
    vi.mocked(getParticipants).mockResolvedValue(pageOf([BOB]));
    const { unmount } = renderSection(false);

    await screen.findByText('Bob');

    expect(screen.getByRole('button', { name: 'Salir del viaje' })).toBeInTheDocument();
    unmount();

    renderSection(true);
    await screen.findByText('Bob');

    expect(screen.queryByRole('button', { name: 'Salir del viaje' })).not.toBeInTheDocument();
  });

  test('asks first and stays on the trip when cancelled', async () => {
    renderRoutes(routes, '/trips/5');

    fireEvent.click(await screen.findByRole('button', { name: 'Salir del viaje' }));
    expect(
      await screen.findByText(
        'Dejarás de ver «Verano en Roma». Para volver necesitarás una nueva invitación.',
      ),
    ).toBeVisible();
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(removeParticipant).not.toHaveBeenCalled();
    expect(screen.queryByText('Lista de viajes')).not.toBeInTheDocument();
  });

  test('confirming removes the logged-in user and goes back to "Mis viajes" with a notice', async () => {
    vi.mocked(removeParticipant).mockResolvedValue(undefined);
    const router = renderRoutes(routes, '/trips/5');

    fireEvent.click(await screen.findByRole('button', { name: 'Salir del viaje' }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.click(within(dialog).getByRole('button', { name: 'Salir del viaje' }));

    expect(await screen.findByText('Lista de viajes')).toBeInTheDocument();
    expect(removeParticipant).toHaveBeenCalledWith(5, ANA.id);
    expect(router.state.location.pathname).toBe('/trips');
    expect(router.state.location.state).toEqual({ notice: 'Has salido de «Verano en Roma».' });
  });
});

describe('I3 ParticipantsSection removing', () => {
  test('offers "Quitar" on every participant to the creator and to nobody else', async () => {
    vi.mocked(getParticipants).mockResolvedValue(pageOf([BOB, CARLA]));
    const { unmount } = renderSection(true);

    await screen.findByText('Bob');

    expect(screen.getByRole('button', { name: 'Quitar a Bob' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Quitar a Carla' })).toBeInTheDocument();
    unmount();

    renderSection(false);
    await screen.findByText('Bob');

    expect(screen.queryByRole('button', { name: /Quitar/ })).not.toBeInTheDocument();
  });

  test('asks first, keeps everything when cancelled and removes nothing', async () => {
    vi.mocked(getParticipants).mockResolvedValue(pageOf([BOB]));
    renderSection(true);

    fireEvent.click(await screen.findByRole('button', { name: 'Quitar a Bob' }));

    expect(
      await screen.findByText(
        'Se quitará a Bob del viaje. Podrás volver a invitar a esta persona.',
      ),
    ).toBeVisible();
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(removeParticipant).not.toHaveBeenCalled();
    expect(screen.getByText('Bob')).toBeInTheDocument();
  });

  test('confirming removes the participant and reloads the list without them', async () => {
    vi.mocked(getParticipants)
      .mockResolvedValueOnce(pageOf([BOB, CARLA]))
      .mockResolvedValueOnce(pageOf([CARLA]));
    vi.mocked(removeParticipant).mockResolvedValue(undefined);
    renderSection(true);

    fireEvent.click(await screen.findByRole('button', { name: 'Quitar a Bob' }));
    fireEvent.click(await screen.findByRole('button', { name: 'Quitar del viaje' }));

    expect(await screen.findByText('Carla')).toBeInTheDocument();
    expect(removeParticipant).toHaveBeenCalledWith(5, 2);
    expect(screen.queryByText('Bob')).not.toBeInTheDocument();
    expect(getParticipants).toHaveBeenCalledTimes(2);
  });
});
