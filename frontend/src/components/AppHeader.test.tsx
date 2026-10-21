import { render, screen } from '@testing-library/react';
import { test, expect } from 'vitest';
import AppHeader from './AppHeader';

test('shows the brand, the navigation links and the user name', () => {
  render(<AppHeader userName="Carlos" />);

  expect(screen.getByText('TripMate')).toBeInTheDocument();
  expect(screen.getByText('Crear viaje')).toBeInTheDocument();
  expect(screen.getByText('Generador IA')).toBeInTheDocument();
  expect(screen.getByText('Carlos')).toBeInTheDocument();
});

test('marks My Trips as the current page', () => {
  render(<AppHeader userName="Carlos" />);

  expect(screen.getByText('Mis viajes')).toHaveAttribute('aria-current', 'page');
});
