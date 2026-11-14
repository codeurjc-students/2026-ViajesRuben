import { screen } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { renderAsUser } from '../renderHelpers';
import { getPublicTrips } from '../services/publicTripService';
import { ANA, pageOf, publicTripOf } from '../testHelpers';
import HomePage from './HomePage';

vi.mock('../services/publicTripService', () => ({
  PUBLIC_PAGE_SIZE: 3,
  CAROUSEL_MAX_PAGES: 3,
  getPublicTrips: vi.fn(),
}));

beforeEach(() => {
  vi.mocked(getPublicTrips)
    .mockReset()
    .mockResolvedValue(pageOf([publicTripOf()], { size: 3 }));
});

describe('B1 HomePage', () => {
  test('introduces TripMate and invites an anonymous visitor to register or log in', async () => {
    renderAsUser(<HomePage />, null);

    expect(
      screen.getByRole('heading', { level: 1, name: 'Planifica tus viajes con TripMate' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Regístrate gratis' })).toHaveAttribute(
      'href',
      '/register',
    );
    expect(screen.getByRole('link', { name: 'Ya tengo cuenta' })).toHaveAttribute('href', '/login');
    expect(screen.queryByRole('link', { name: 'Ir a Mis viajes' })).not.toBeInTheDocument();
    await screen.findByRole('heading', { name: 'Roma en cinco días' });
  });

  test('shows the public itineraries with their author and a link to see them all', async () => {
    renderAsUser(<HomePage />, null);

    expect(await screen.findByRole('heading', { name: 'Roma en cinco días' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Itinerarios públicos' })).toBeInTheDocument();
    expect(screen.getByText('Por Carlos')).toBeInTheDocument();
    expect(
      screen.getByRole('link', { name: 'Ver itinerario completo de Roma en cinco días' }),
    ).toHaveAttribute('href', '/public/10');
    expect(screen.getByRole('link', { name: 'Ver todos' })).toHaveAttribute('href', '/public');
  });

  test('explains in three steps what exists in this version', async () => {
    renderAsUser(<HomePage />, null);

    const how = screen.getByRole('region', { name: 'Cómo funciona' });
    const steps = how.querySelectorAll('ol > li');
    expect(steps).toHaveLength(3);
    expect(steps[0]).toHaveTextContent('Crea un viaje');
    expect(steps[1]).toHaveTextContent('Añade actividades día a día');
    expect(steps[2]).toHaveTextContent('Mantén el itinerario sin conflictos');
    expect(how).not.toHaveTextContent(/invitaci|invitar|generador|\bIA\b/);
    await screen.findByRole('heading', { name: 'Roma en cinco días' });
  });

  test('a logged-in user sees the same page with a link to their trips instead', async () => {
    renderAsUser(<HomePage />, ANA);

    expect(screen.getByRole('link', { name: 'Ir a Mis viajes' })).toHaveAttribute('href', '/trips');
    expect(screen.queryByRole('link', { name: 'Regístrate gratis' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Ya tengo cuenta' })).not.toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'Roma en cinco días' })).toBeInTheDocument();
  });
});
