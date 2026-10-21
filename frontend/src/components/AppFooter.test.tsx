import { render, screen } from '@testing-library/react';
import { test, expect } from 'vitest';
import AppFooter from './AppFooter';

test('shows the brand, the project and the current year', () => {
  render(<AppFooter />);

  expect(screen.getByRole('contentinfo')).toBeInTheDocument();
  expect(screen.getByText('TripMate')).toBeInTheDocument();
  expect(screen.getByText('Proyecto de fin de grado, ETSII-URJC')).toBeInTheDocument();
  expect(screen.getByText(`© ${new Date().getFullYear()} TripMate`)).toBeInTheDocument();
});
