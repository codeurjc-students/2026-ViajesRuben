import { render, screen } from '@testing-library/react';
import { test, expect } from 'vitest';
import PlaceholderPage from './PlaceholderPage';

test('shows the title and that the screen is coming', () => {
  render(<PlaceholderPage title="Perfil" />);

  expect(screen.getByRole('heading', { name: 'Perfil' })).toBeInTheDocument();
  expect(screen.getByText('Esta pantalla estará disponible próximamente.')).toBeInTheDocument();
});
