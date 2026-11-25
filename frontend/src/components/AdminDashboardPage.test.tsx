import { screen, within } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import AdminDashboardPage from './AdminDashboardPage';
import { renderAsUser, renderRoutes } from '../renderHelpers';
import { getMetrics } from '../services/adminMetricsService';
import { ApiError } from '../services/apiError';
import { ANA } from '../testHelpers';
import type { User } from '../types';

vi.mock('../services/adminMetricsService', () => ({ getMetrics: vi.fn() }));

const ADMIN: User = { ...ANA, id: 9, name: 'Administrator', role: 'ADMIN' };

beforeEach(() => {
  vi.mocked(getMetrics).mockReset();
});

describe('I6 AdminDashboardPage', () => {
  test('shows the title, a loading spinner and then the three totals', async () => {
    vi.mocked(getMetrics).mockResolvedValue({ users: 3, trips: 5, activities: 12 });

    renderAsUser(<AdminDashboardPage />, ADMIN, '/admin');

    expect(
      screen.getByRole('heading', { name: 'Panel de administración', level: 1 }),
    ).toBeInTheDocument();
    expect(screen.getByText('Cargando métricas…')).toBeInTheDocument();
    const cards = await screen.findAllByRole('listitem');
    expect(cards).toHaveLength(3);
    expect(within(cards[0]).getByRole('heading', { name: 'Usuarios' })).toBeInTheDocument();
    expect(within(cards[0]).getByText('3')).toBeInTheDocument();
    expect(within(cards[1]).getByRole('heading', { name: 'Viajes' })).toBeInTheDocument();
    expect(within(cards[1]).getByText('5')).toBeInTheDocument();
    expect(within(cards[2]).getByRole('heading', { name: 'Actividades' })).toBeInTheDocument();
    expect(within(cards[2]).getByText('12')).toBeInTheDocument();
    expect(screen.queryByText('Cargando métricas…')).not.toBeInTheDocument();
    expect(getMetrics).toHaveBeenCalledOnce();
  });

  test('writes the big numbers with the Spanish thousands separator and shows a zero', async () => {
    vi.mocked(getMetrics).mockResolvedValue({ users: 0, trips: 12345, activities: 1234567 });

    renderAsUser(<AdminDashboardPage />, ADMIN, '/admin');

    expect(await screen.findByText('0')).toBeInTheDocument();
    expect(screen.getByText('12.345')).toBeInTheDocument();
    expect(screen.getByText('1.234.567')).toBeInTheDocument();
  });

  test('links to the user management and the trip moderation pages', async () => {
    vi.mocked(getMetrics).mockResolvedValue({ users: 3, trips: 5, activities: 12 });

    renderAsUser(<AdminDashboardPage />, ADMIN, '/admin');
    await screen.findAllByRole('listitem');

    expect(screen.getByRole('link', { name: 'Gestionar usuarios' })).toHaveAttribute(
      'href',
      '/admin/users',
    );
    expect(screen.getByRole('link', { name: 'Moderar viajes' })).toHaveAttribute(
      'href',
      '/admin/trips',
    );
  });

  test('shows the links while the totals are still loading', () => {
    vi.mocked(getMetrics).mockReturnValue(new Promise(() => {}));

    renderAsUser(<AdminDashboardPage />, ADMIN, '/admin');

    expect(screen.getByRole('link', { name: 'Gestionar usuarios' })).toBeInTheDocument();
    expect(screen.queryByRole('listitem')).not.toBeInTheDocument();
  });

  test('hands a loading failure to the route error page', async () => {
    vi.mocked(getMetrics).mockRejectedValue(new ApiError(500));

    renderRoutes(
      [{ path: '/', element: <AdminDashboardPage />, errorElement: <p>Página de error</p> }],
      '/',
      ADMIN,
    );

    expect(await screen.findByText('Página de error')).toBeInTheDocument();
  });
});
