import { fireEvent, screen, waitFor } from '@testing-library/react';
import { vi, test, expect, beforeEach } from 'vitest';
import TripList from './TripList';
import { renderAsUser, renderRoutes } from '../renderHelpers';
import { getMyInvitations, respondToInvitation } from '../services/invitationService';
import { deleteTrip, getTrips } from '../services/tripService';
import { pageOf, receivedInvitationOf, tripOf, tripsFrom } from '../testHelpers';

vi.mock('../services/tripService', () => ({ getTrips: vi.fn(), deleteTrip: vi.fn() }));
vi.mock('../services/invitationService', () => ({
  getMyInvitations: vi.fn(),
  respondToInvitation: vi.fn(),
}));

beforeEach(() => {
  vi.mocked(getTrips).mockReset();
  vi.mocked(deleteTrip).mockReset();
  vi.mocked(getMyInvitations).mockReset().mockResolvedValue(pageOf([]));
  vi.mocked(respondToInvitation).mockReset();
});

test('renders the trips of the user by name', async () => {
  vi.mocked(getTrips).mockResolvedValue(
    pageOf([
      tripOf({ id: 1, name: 'Escapada a Lisboa', destination: 'Lisboa' }),
      tripOf({ id: 2, name: 'Verano en Roma', destination: 'Roma' }),
    ]),
  );

  renderAsUser(<TripList />);

  expect(await screen.findByText('Escapada a Lisboa')).toBeInTheDocument();
  expect(screen.getByText('Verano en Roma')).toBeInTheDocument();
  expect(screen.getByText('Destino: Roma')).toBeInTheDocument();
});

test('shows the invitation to add the first trip when there are none', async () => {
  vi.mocked(getTrips).mockResolvedValue(pageOf([]));

  renderAsUser(<TripList />);

  expect(await screen.findByRole('link', { name: 'Añadir tu primer viaje' })).toHaveAttribute(
    'href',
    '/trips/new',
  );
  expect(screen.queryByRole('list')).not.toBeInTheDocument();
});

test('hands a loading failure to the route error page', async () => {
  vi.mocked(getTrips).mockRejectedValue(new Error('Network error'));

  renderRoutes([{ path: '/', element: <TripList />, errorElement: <p>Página de error</p> }], '/');

  expect(await screen.findByText('Página de error')).toBeInTheDocument();
  expect(screen.queryByRole('list')).not.toBeInTheDocument();
});

test('B4 "Nuevo viaje" is a link to the creation page', async () => {
  vi.mocked(getTrips).mockResolvedValue(pageOf([]));

  renderAsUser(<TripList />);

  expect(await screen.findByRole('link', { name: 'Nuevo viaje' })).toHaveAttribute(
    'href',
    '/trips/new',
  );
});

test('shows how many trips the user has', async () => {
  vi.mocked(getTrips).mockResolvedValue(pageOf([tripOf()]));

  renderAsUser(<TripList />);

  expect(await screen.findByText('1 viaje')).toBeInTheDocument();
});

test('appends the next page when "Más resultados" is clicked and hides the button on the last one', async () => {
  vi.mocked(getTrips)
    .mockResolvedValueOnce(pageOf(tripsFrom(1, 10), { page: 0, totalElements: 12, last: false }))
    .mockResolvedValueOnce(pageOf(tripsFrom(11, 2), { page: 1, totalElements: 12, last: true }));

  renderAsUser(<TripList />);

  expect(await screen.findByText('Trip 1')).toBeInTheDocument();
  expect(screen.getAllByRole('listitem')).toHaveLength(10);
  expect(screen.getByText('12 viajes')).toBeInTheDocument();

  fireEvent.click(screen.getByRole('button', { name: 'Más resultados' }));

  expect(await screen.findByText('Trip 12')).toBeInTheDocument();
  expect(screen.getAllByRole('listitem')).toHaveLength(12);
  expect(getTrips).toHaveBeenLastCalledWith(1);
  expect(screen.queryByRole('button', { name: 'Más resultados' })).not.toBeInTheDocument();
});

test('does not offer more results when everything fits in one page', async () => {
  vi.mocked(getTrips).mockResolvedValue(pageOf(tripsFrom(1, 3)));

  renderAsUser(<TripList />);

  expect(await screen.findByText('Trip 1')).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Más resultados' })).not.toBeInTheDocument();
});

test('B4 deleting a trip from its card reloads the list from the first page', async () => {
  vi.mocked(getTrips)
    .mockResolvedValueOnce(pageOf(tripsFrom(1, 2)))
    .mockResolvedValueOnce(pageOf(tripsFrom(2, 1)));
  vi.mocked(deleteTrip).mockResolvedValue(undefined);
  renderAsUser(<TripList />);
  await screen.findByText('Trip 1');

  fireEvent.click(screen.getByRole('button', { name: 'Eliminar Trip 1' }));
  fireEvent.click(screen.getByRole('button', { name: 'Eliminar viaje' }));

  await waitFor(() => expect(screen.queryByText('Trip 1')).not.toBeInTheDocument());
  expect(await screen.findByText('Trip 2')).toBeInTheDocument();
  expect(deleteTrip).toHaveBeenCalledWith(1);
  expect(getTrips).toHaveBeenCalledTimes(2);
  expect(getTrips).toHaveBeenLastCalledWith(0);
  expect(await screen.findByText('1 viaje')).toBeInTheDocument();
});

test('I2 shows the pending invitations above the trips', async () => {
  vi.mocked(getTrips).mockResolvedValue(pageOf([tripOf({ id: 1, name: 'Escapada a Lisboa' })]));
  vi.mocked(getMyInvitations).mockResolvedValue(
    pageOf([receivedInvitationOf({ tripName: 'Verano en Roma' })]),
  );

  renderAsUser(<TripList />);

  const heading = await screen.findByRole('heading', { name: 'Invitaciones pendientes' });
  const trips = await screen.findByText('Escapada a Lisboa');
  expect(heading.compareDocumentPosition(trips) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
});

test('I2 does not show the section when there are no pending invitations', async () => {
  vi.mocked(getTrips).mockResolvedValue(pageOf([tripOf({ id: 1, name: 'Escapada a Lisboa' })]));

  renderAsUser(<TripList />);

  await screen.findByText('Escapada a Lisboa');
  expect(
    screen.queryByRole('heading', { name: 'Invitaciones pendientes' }),
  ).not.toBeInTheDocument();
});

test('I2 after accepting an invitation the trip list is loaded again and shows the new trip', async () => {
  vi.mocked(getTrips)
    .mockResolvedValueOnce(pageOf([tripOf({ id: 1, name: 'Escapada a Lisboa' })]))
    .mockResolvedValueOnce(
      pageOf([
        tripOf({ id: 1, name: 'Escapada a Lisboa' }),
        tripOf({ id: 5, name: 'Verano en Roma', creatorId: 99 }),
      ]),
    );
  const invitation = receivedInvitationOf({ id: 3, tripId: 5, tripName: 'Verano en Roma' });
  vi.mocked(getMyInvitations).mockResolvedValue(pageOf([invitation]));
  vi.mocked(respondToInvitation).mockResolvedValue({ ...invitation, status: 'ACCEPTED' });
  renderAsUser(<TripList />);

  fireEvent.click(
    await screen.findByRole('button', { name: 'Aceptar invitación a Verano en Roma' }),
  );

  expect(await screen.findByRole('link', { name: 'Verano en Roma' })).toHaveAttribute(
    'href',
    '/trips/5',
  );
  expect(getTrips).toHaveBeenCalledTimes(2);
  expect(screen.queryByRole('button', { name: 'Eliminar Verano en Roma' })).not.toBeInTheDocument();
});

test('I3 shows the notice left by the page that sent the user here, and lets them dismiss it', async () => {
  vi.mocked(getTrips).mockResolvedValue(pageOf([]));
  renderRoutes([{ path: '/', element: <TripList /> }], '/', undefined, {
    notice: 'Has salido de «Verano en Roma».',
  });

  expect(await screen.findByText('Has salido de «Verano en Roma».')).toBeVisible();

  fireEvent.click(screen.getByRole('button', { name: /close/i }));

  expect(screen.queryByText('Has salido de «Verano en Roma».')).not.toBeInTheDocument();
});

test('I3 shows no notice when the user arrives without one', async () => {
  vi.mocked(getTrips).mockResolvedValue(pageOf([]));
  renderAsUser(<TripList />);

  await screen.findByRole('link', { name: 'Añadir tu primer viaje' });

  expect(screen.queryByRole('alert')).not.toBeInTheDocument();
});
