import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeAll, expect, test, vi } from 'vitest';
import { newUser } from '../integrationHelpers';
import { createTrip } from '../services/tripService';
import TripList from './TripList';

// A fresh user, so the test never depends on (or changes) the sample users.
beforeAll(async () => {
  await newUser();
  for (let i = 1; i <= 12; i++) {
    await createTrip({
      name: `Viaje ${i}`,
      destination: `Destino ${i}`,
      startDate: `2027-05-${String(i).padStart(2, '0')}`,
      endDate: `2027-06-${String(i).padStart(2, '0')}`,
      budget: null,
      description: null,
    });
  }
});

afterEach(() => {
  // newUser() installs the fetch spy once; it must stay for the whole file.
  vi.clearAllMocks();
});

test('B4 renders the first trips of the user fetched from the REST API', async () => {
  render(<TripList />);

  expect(await screen.findByText('Viaje a Destino 1', {}, { timeout: 5000 })).toBeInTheDocument();
  expect(screen.getByText('Viaje a Destino 2')).toBeInTheDocument();
  expect(screen.getAllByRole('listitem')).toHaveLength(10);
});

test('B4 loads the next page from the REST API with "Más resultados"', async () => {
  render(<TripList />);
  await screen.findByText('Viaje a Destino 1', {}, { timeout: 5000 });

  fireEvent.click(screen.getByRole('button', { name: 'Más resultados' }));

  await waitFor(() => expect(screen.getAllByRole('listitem')).toHaveLength(12));
  expect(screen.queryByRole('button', { name: 'Más resultados' })).not.toBeInTheDocument();
});
