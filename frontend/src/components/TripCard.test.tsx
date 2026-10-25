import { render, screen } from '@testing-library/react';
import { test, expect } from 'vitest';
import { DEFAULT_TRIP_COVER } from '../constants/images';
import { tripOf } from '../testHelpers';
import TripCard from './TripCard';

const trip = tripOf({
  id: 1,
  name: 'Trip 2',
  destination: 'Rome',
  startDate: '2027-07-10',
  endDate: '2027-07-17',
  participantCount: 6,
  isCreator: true,
});

test('shows the trip name, dates and participants', () => {
  render(<TripCard trip={trip} />);

  expect(screen.getByRole('heading', { name: 'Viaje a Rome' })).toBeInTheDocument();
  expect(screen.getByText('Fechas: 10 jul 2027 – 17 jul 2027')).toBeInTheDocument();
  expect(screen.getByText('6 participantes')).toBeInTheDocument();
});

test('uses the singular for one participant', () => {
  render(<TripCard trip={{ ...trip, participantCount: 1 }} />);

  expect(screen.getByText('1 participante')).toBeInTheDocument();
});

test('shows delete and edit buttons to the trip creator', () => {
  render(<TripCard trip={trip} />);

  expect(screen.getByRole('button', { name: 'Eliminar Viaje a Rome' })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Editar Viaje a Rome' })).toBeInTheDocument();
});

test('hides the edit button from participants who are not the creator', () => {
  render(<TripCard trip={{ ...trip, isCreator: false }} />);

  expect(screen.getByRole('button', { name: 'Eliminar Viaje a Rome' })).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Editar Viaje a Rome' })).not.toBeInTheDocument();
});

test('shows the default cover while trips have no image of their own', () => {
  const { container } = render(<TripCard trip={trip} />);

  expect(container.querySelector('img')).toHaveAttribute('src', DEFAULT_TRIP_COVER);
});
