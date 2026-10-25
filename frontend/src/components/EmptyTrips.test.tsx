import { screen } from '@testing-library/react';
import { expect, test } from 'vitest';
import { DEFAULT_TRIP_COVER } from '../constants/images';
import { renderAsUser } from '../renderHelpers';
import EmptyTrips from './EmptyTrips';

test('B4 invites to add the first trip with a sketch of a trip card', () => {
  const { container } = renderAsUser(<EmptyTrips />);

  expect(
    screen.getByRole('heading', { name: 'Tu primer viaje te está esperando' }),
  ).toBeInTheDocument();
  expect(screen.getByText('Destino: ¿A dónde te apetece ir?')).toBeInTheDocument();
  expect(container.querySelector('img')).toHaveAttribute('src', DEFAULT_TRIP_COVER);
});

test('B4 the main action opens the new trip form', () => {
  renderAsUser(<EmptyTrips />);

  expect(screen.getByRole('link', { name: 'Añadir tu primer viaje' })).toHaveAttribute(
    'href',
    '/trips/new',
  );
});
