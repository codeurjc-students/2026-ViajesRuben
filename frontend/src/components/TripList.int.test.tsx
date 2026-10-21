import { render, screen } from '@testing-library/react';
import { test, expect } from 'vitest';
import TripList from './TripList';

test('renders trips fetched from the REST API', async () => {
  render(<TripList />);

  expect(await screen.findByText('Viaje a Lisbon', {}, { timeout: 5000 })).toBeInTheDocument();
  expect(screen.getByText('Viaje a Rome')).toBeInTheDocument();
});
