import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { test, expect } from 'vitest';
import AppHeader from './AppHeader';

function renderAt(path: string) {
  render(
    <MemoryRouter initialEntries={[path]}>
      <AppHeader userName="Carlos" />
    </MemoryRouter>,
  );
}

test('shows the brand, the navigation links and the user name', () => {
  renderAt('/trips');

  expect(screen.getByText('TripMate')).toBeInTheDocument();
  expect(screen.getByText('Crear viaje')).toBeInTheDocument();
  expect(screen.getByText('Generador IA')).toBeInTheDocument();
  expect(screen.getByText('Carlos')).toBeInTheDocument();
});

test('the brand links to the home page and the menu to the trip screens', () => {
  renderAt('/trips');

  expect(screen.getByRole('link', { name: 'TripMate' })).toHaveAttribute('href', '/');
  expect(screen.getByRole('link', { name: 'Mis viajes' })).toHaveAttribute('href', '/trips');
  expect(screen.getByRole('link', { name: 'Crear viaje' })).toHaveAttribute('href', '/trips/new');
});

test('marks My Trips as the current page on /trips', () => {
  renderAt('/trips');

  expect(screen.getByText('Mis viajes')).toHaveAttribute('aria-current', 'page');
  expect(screen.getByText('Crear viaje')).not.toHaveAttribute('aria-current');
});

test('marks Create trip as the current page on /trips/new', () => {
  renderAt('/trips/new');

  expect(screen.getByText('Crear viaje')).toHaveAttribute('aria-current', 'page');
  expect(screen.getByText('Mis viajes')).not.toHaveAttribute('aria-current');
});
