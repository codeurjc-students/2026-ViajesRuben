import { fireEvent, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { DEFAULT_TRIP_COVER } from '../constants/images';
import { renderAsUser } from '../renderHelpers';
import { deleteTrip } from '../services/tripService';
import { ANA, tripOf } from '../testHelpers';
import TripCard from './TripCard';

vi.mock('../services/tripService', () => ({ deleteTrip: vi.fn() }));

const trip = tripOf({
  id: 3,
  creatorId: ANA.id,
  name: 'Verano en Roma',
  destination: 'Roma',
  startDate: '2027-07-10',
  endDate: '2027-07-17',
});

beforeEach(() => {
  vi.mocked(deleteTrip).mockReset();
});

describe('B4 TripCard', () => {
  test('shows the name as a link to the detail, the destination and the dates', () => {
    renderAsUser(<TripCard trip={trip} onDeleted={vi.fn()} />);

    expect(screen.getByRole('heading', { name: 'Verano en Roma' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Verano en Roma' })).toHaveAttribute(
      'href',
      '/trips/3',
    );
    expect(screen.getByText('Destino: Roma')).toBeInTheDocument();
    expect(screen.getByText('Fechas: 10 jul 2027 – 17 jul 2027')).toBeInTheDocument();
  });

  test('I3 shows how many people joined the trip, zero included', () => {
    const { unmount } = renderAsUser(<TripCard trip={trip} onDeleted={vi.fn()} />);
    expect(screen.getByText('Participantes: 0')).toBeInTheDocument();
    unmount();

    renderAsUser(<TripCard trip={{ ...trip, participantCount: 4 }} onDeleted={vi.fn()} />);

    expect(screen.getByText('Participantes: 4')).toBeInTheDocument();
    expect(screen.queryByText('Participantes: 0')).not.toBeInTheDocument();
  });

  test('I3 shows the same number to a participant, who is not the creator', () => {
    renderAsUser(
      <TripCard
        trip={{ ...trip, creatorId: ANA.id + 1, participantCount: 2 }}
        onDeleted={vi.fn()}
      />,
    );

    expect(screen.getByText('Participantes: 2')).toBeInTheDocument();
  });

  test('I7 a published trip shows the "Público" badge and an unpublished one does not', () => {
    const { unmount } = renderAsUser(
      <TripCard
        trip={{ ...trip, published: true, publishedAt: '2027-01-01T10:00:00Z' }}
        onDeleted={vi.fn()}
      />,
    );
    expect(screen.getByText('Público')).toBeInTheDocument();
    unmount();

    renderAsUser(<TripCard trip={trip} onDeleted={vi.fn()} />);

    expect(screen.queryByText('Público')).not.toBeInTheDocument();
  });

  test('shows the delete button and the edit link to the creator', () => {
    renderAsUser(<TripCard trip={trip} onDeleted={vi.fn()} />);

    expect(screen.getByRole('button', { name: 'Eliminar Verano en Roma' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Editar Verano en Roma' })).toHaveAttribute(
      'href',
      '/trips/3/edit',
    );
  });

  test('I2 the card of a joined trip links to it with no edit or delete action', () => {
    renderAsUser(<TripCard trip={{ ...trip, creatorId: ANA.id + 1 }} onDeleted={vi.fn()} />);

    expect(screen.getByRole('link', { name: 'Verano en Roma' })).toHaveAttribute(
      'href',
      '/trips/3',
    );
    expect(
      screen.queryByRole('button', { name: 'Eliminar Verano en Roma' }),
    ).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Editar Verano en Roma' })).not.toBeInTheDocument();
  });

  test('hides both actions from anyone who is not the creator', () => {
    renderAsUser(<TripCard trip={{ ...trip, creatorId: ANA.id + 1 }} onDeleted={vi.fn()} />);

    expect(
      screen.queryByRole('button', { name: 'Eliminar Verano en Roma' }),
    ).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Editar Verano en Roma' })).not.toBeInTheDocument();
  });

  test('shows the default cover when the trip has none and its own cover when it has one', () => {
    const { container, unmount } = renderAsUser(<TripCard trip={trip} onDeleted={vi.fn()} />);
    expect(container.querySelector('img')).toHaveAttribute('src', DEFAULT_TRIP_COVER);
    unmount();

    const withCover = renderAsUser(
      <TripCard trip={{ ...trip, coverImageId: 'abc-123' }} onDeleted={vi.fn()} />,
    );

    expect(withCover.container.querySelector('img')).toHaveAttribute(
      'src',
      '/api/v1/images/abc-123',
    );
  });

  test('asks for confirmation before deleting and does nothing when cancelled', () => {
    renderAsUser(<TripCard trip={trip} onDeleted={vi.fn()} />);

    fireEvent.click(screen.getByRole('button', { name: 'Eliminar Verano en Roma' }));
    expect(screen.getByRole('dialog', { name: 'Eliminar viaje' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(deleteTrip).not.toHaveBeenCalled();
  });

  test('deletes the trip once confirmed and tells the list', async () => {
    vi.mocked(deleteTrip).mockResolvedValue(undefined);
    const onDeleted = vi.fn();
    renderAsUser(<TripCard trip={trip} onDeleted={onDeleted} />);

    fireEvent.click(screen.getByRole('button', { name: 'Eliminar Verano en Roma' }));
    fireEvent.click(screen.getByRole('button', { name: 'Eliminar viaje' }));

    await waitFor(() => expect(onDeleted).toHaveBeenCalledTimes(1));
    expect(deleteTrip).toHaveBeenCalledWith(3);
  });
});
