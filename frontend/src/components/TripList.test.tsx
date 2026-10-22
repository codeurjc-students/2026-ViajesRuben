import { fireEvent, render, screen } from '@testing-library/react';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { vi, test, expect } from 'vitest';
import TripList from './TripList';
import { getTrips } from '../services/tripService';
import { pageOf, tripsFrom } from '../testHelpers';

vi.mock('../services/tripService', () => ({ getTrips: vi.fn() }));

test('renders the list of trips', async () => {
  vi.mocked(getTrips).mockResolvedValue(
    pageOf([
      {
        id: 1,
        name: 'Trip 1',
        destination: 'Lisbon',
        startDate: '2027-05-01',
        endDate: '2027-05-05',
      },
      {
        id: 2,
        name: 'Trip 2',
        destination: 'Rome',
        startDate: '2027-07-10',
        endDate: '2027-07-17',
      },
    ]),
  );

  render(<TripList />);

  expect(await screen.findByText('Viaje a Lisbon')).toBeInTheDocument();
  expect(screen.getByText('Viaje a Rome')).toBeInTheDocument();
});

test('shows an invitation to create a trip when there are none', async () => {
  vi.mocked(getTrips).mockResolvedValue(pageOf([]));

  render(<TripList />);

  expect(await screen.findByText('Aún no tienes viajes. Crea el primero.')).toBeInTheDocument();
  expect(screen.queryByRole('list')).not.toBeInTheDocument();
});

test('hands a loading failure to the route error page', async () => {
  vi.mocked(getTrips).mockRejectedValue(new Error('Network error'));
  const router = createMemoryRouter([
    { path: '/', element: <TripList />, errorElement: <p>Página de error</p> },
  ]);

  render(<RouterProvider router={router} />);

  expect(await screen.findByText('Página de error')).toBeInTheDocument();
  expect(screen.queryByRole('list')).not.toBeInTheDocument();
});

test('offers a button to create a new trip', async () => {
  vi.mocked(getTrips).mockResolvedValue(pageOf([]));

  render(<TripList />);

  expect(await screen.findByRole('button', { name: 'Crear nuevo viaje' })).toBeInTheDocument();
});

test('shows how many trips the user has', async () => {
  vi.mocked(getTrips).mockResolvedValue(
    pageOf([
      {
        id: 1,
        name: 'Trip 1',
        destination: 'Lisbon',
        startDate: '2027-05-01',
        endDate: '2027-05-05',
      },
    ]),
  );

  render(<TripList />);

  expect(await screen.findByText('1 viaje')).toBeInTheDocument();
});

test('appends the next page when "Más resultados" is clicked and hides the button on the last one', async () => {
  vi.mocked(getTrips)
    .mockResolvedValueOnce(pageOf(tripsFrom(1, 10), { page: 0, totalElements: 12, last: false }))
    .mockResolvedValueOnce(pageOf(tripsFrom(11, 2), { page: 1, totalElements: 12, last: true }));

  render(<TripList />);

  expect(await screen.findByText('Viaje a City 1')).toBeInTheDocument();
  expect(screen.getAllByRole('listitem')).toHaveLength(10);
  expect(screen.getByText('12 viajes')).toBeInTheDocument();

  fireEvent.click(screen.getByRole('button', { name: 'Más resultados' }));

  expect(await screen.findByText('Viaje a City 12')).toBeInTheDocument();
  expect(screen.getAllByRole('listitem')).toHaveLength(12);
  expect(getTrips).toHaveBeenLastCalledWith(1);
  expect(screen.queryByRole('button', { name: 'Más resultados' })).not.toBeInTheDocument();
});

test('does not offer more results when everything fits in one page', async () => {
  vi.mocked(getTrips).mockResolvedValue(pageOf(tripsFrom(1, 3)));

  render(<TripList />);

  expect(await screen.findByText('Viaje a City 1')).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Más resultados' })).not.toBeInTheDocument();
});
