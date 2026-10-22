import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { test, expect } from 'vitest';
import TripList from './TripList';

test('renders the first trips fetched from the REST API', async () => {
  render(<TripList />);

  expect(await screen.findByText('Viaje a Lisbon', {}, { timeout: 5000 })).toBeInTheDocument();
  expect(screen.getByText('Viaje a Rome')).toBeInTheDocument();
});

test('loads the next page from the REST API with "Más resultados"', async () => {
  render(<TripList />);
  await screen.findByText('Viaje a Lisbon', {}, { timeout: 5000 });
  const firstPage = screen.getAllByRole('listitem').length;
  expect(firstPage).toBe(10);

  fireEvent.click(screen.getByRole('button', { name: 'Más resultados' }));

  await waitFor(() => expect(screen.getAllByRole('listitem').length).toBeGreaterThan(firstPage));
  expect(screen.queryByRole('button', { name: 'Más resultados' })).not.toBeInTheDocument();
});
