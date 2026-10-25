import { fireEvent, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeAll, expect, test, vi } from 'vitest';
import { newUser } from '../integrationHelpers';
import { renderAsUser } from '../renderHelpers';
import { createTrip } from '../services/tripService';
import type { User } from '../types';
import TripList from './TripList';

let user: User;

// A fresh user, so the test never depends on (or changes) the sample users.
beforeAll(async () => {
  user = (await newUser()).user;
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
  renderAsUser(<TripList />, user);

  expect(await screen.findByText('Viaje 1', {}, { timeout: 5000 })).toBeInTheDocument();
  expect(screen.getByText('Viaje 2')).toBeInTheDocument();
  expect(screen.getAllByRole('listitem')).toHaveLength(10);
});

test('B4 loads the next page from the REST API with "Más resultados"', async () => {
  renderAsUser(<TripList />, user);
  await screen.findByText('Viaje 1', {}, { timeout: 5000 });

  fireEvent.click(screen.getByRole('button', { name: 'Más resultados' }));

  await waitFor(() => expect(screen.getAllByRole('listitem')).toHaveLength(12));
  expect(screen.queryByRole('button', { name: 'Más resultados' })).not.toBeInTheDocument();
});
