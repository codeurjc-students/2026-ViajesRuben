import { render, screen } from '@testing-library/react';
import { vi, test, expect } from 'vitest';
import App from './App';
import { getTrips } from './services/tripService';

vi.mock('./services/tripService', () => ({ getTrips: vi.fn() }));

test('renders the trip list on the home page', async () => {
  vi.mocked(getTrips).mockResolvedValue([
    {
      id: 1,
      name: 'Trip 1',
      destination: 'Lisbon',
      startDate: '2027-05-01',
      endDate: '2027-05-05',
    },
  ]);

  render(<App />);

  expect(await screen.findByText('Viaje a Lisbon')).toBeInTheDocument();
  expect(screen.getByRole('navigation')).toBeInTheDocument();
  expect(screen.getByRole('contentinfo')).toBeInTheDocument();
});
