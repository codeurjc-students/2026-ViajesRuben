import { fireEvent, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { renderAsUser } from '../renderHelpers';
import { ApiError, NetworkError } from '../services/apiError';
import { deleteActivity } from '../services/activityService';
import { activityOf } from '../testHelpers';
import ActivityCard from './ActivityCard';
import DeleteActivityModal from './DeleteActivityModal';

vi.mock('../services/activityService', () => ({ deleteActivity: vi.fn() }));

beforeEach(() => {
  vi.mocked(deleteActivity).mockReset();
});

function renderCard(overrides = {}, canEdit = true, onDeleted = vi.fn()) {
  renderAsUser(
    <ol>
      <ActivityCard activity={activityOf(overrides)} canEdit={canEdit} onDeleted={onDeleted} />
    </ol>,
  );
  return { onDeleted };
}

describe('B5 ActivityCard', () => {
  test('shows the schedule, duration, name, category, location, cost and description', () => {
    renderCard({
      name: 'Torre de Belém',
      startTime: '10:00:00',
      endTime: '12:30:00',
      category: 'SIGHTSEEING',
      location: 'Belém',
      cost: 6,
      description: 'Subida a la torre.',
    });

    expect(screen.getByRole('heading', { name: 'Torre de Belém' })).toBeInTheDocument();
    expect(screen.getByText('10:00 – 12:30')).toBeInTheDocument();
    expect(screen.getByText('2 h 30 min')).toBeInTheDocument();
    expect(screen.getByText(/Visita — Belém/)).toBeInTheDocument();
    expect(screen.getByText(/^6,00\s€$/)).toBeInTheDocument();
    expect(screen.getByText('Subida a la torre.')).toBeInTheDocument();
  });

  test('an activity without a schedule lasts all day', () => {
    renderCard({ startTime: null, endTime: null });

    expect(screen.getByText('Todo el día')).toBeInTheDocument();
    expect(screen.queryByText(/min$/)).not.toBeInTheDocument();
  });

  test('an activity with only a start shows just that time', () => {
    renderCard({ startTime: '21:00:00', endTime: null });

    expect(screen.getByText('21:00')).toBeInTheDocument();
  });

  test('leaves out what the activity does not have', () => {
    renderCard();

    expect(screen.queryByText(/€/)).not.toBeInTheDocument();
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
    expect(screen.queryByText('Visita')).not.toBeInTheDocument();
  });

  test('a free activity shows 0,00 €', () => {
    renderCard({ cost: 0 });

    expect(screen.getByText(/^0,00\s€$/)).toBeInTheDocument();
  });

  test('shows the image of the activity', () => {
    renderCard({ imageId: 'img-1', name: 'Torre' });

    expect(screen.getByAltText('Imagen de Torre')).toHaveAttribute('src', '/api/v1/images/img-1');
  });

  test('hides the image when it fails to load', () => {
    renderCard({ imageId: 'img-1', name: 'Torre' });

    fireEvent.error(screen.getByAltText('Imagen de Torre'));

    expect(screen.queryByAltText('Imagen de Torre')).not.toBeInTheDocument();
  });

  test('the creator can edit and delete', () => {
    renderCard({ id: 4, tripId: 2, name: 'Torre' });

    expect(screen.getByRole('link', { name: 'Editar Torre' })).toHaveAttribute(
      'href',
      '/trips/2/activities/4/edit',
    );
    expect(screen.getByRole('button', { name: 'Eliminar Torre' })).toBeInTheDocument();
  });

  test('anyone else only reads', () => {
    renderCard({ name: 'Torre' }, false);

    expect(screen.queryByRole('link', { name: 'Editar Torre' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Eliminar Torre' })).not.toBeInTheDocument();
  });

  test('deleting asks first, keeps the activity when cancelled and reports it when confirmed', async () => {
    vi.mocked(deleteActivity).mockResolvedValue(undefined);
    const { onDeleted } = renderCard({ id: 4, tripId: 2, name: 'Torre' });
    fireEvent.click(screen.getByRole('button', { name: 'Eliminar Torre' }));
    expect(screen.getByRole('dialog', { name: 'Eliminar actividad' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));
    expect(deleteActivity).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', { name: 'Eliminar Torre' }));
    fireEvent.click(screen.getByRole('button', { name: 'Eliminar actividad' }));

    await waitFor(() => expect(onDeleted).toHaveBeenCalledTimes(1));
    expect(deleteActivity).toHaveBeenCalledWith(2, 4);
  });
});

describe('B5 DeleteActivityModal', () => {
  const activity = activityOf({ id: 4, tripId: 2, name: 'Torre' });

  function renderModal() {
    const onCancel = vi.fn();
    const onDeleted = vi.fn();
    renderAsUser(
      <DeleteActivityModal activity={activity} show onCancel={onCancel} onDeleted={onDeleted} />,
    );
    return { onCancel, onDeleted };
  }

  test('names the activity and warns that it cannot be undone', () => {
    renderModal();

    expect(screen.getByRole('dialog', { name: 'Eliminar actividad' })).toHaveTextContent(
      'Se eliminará «Torre» con su imagen. Esta acción no se puede deshacer.',
    );
  });

  test('shows the busy label while the request is running', async () => {
    let finish: () => void = () => {};
    vi.mocked(deleteActivity).mockReturnValue(
      new Promise<void>((resolve) => {
        finish = resolve;
      }),
    );
    renderModal();

    fireEvent.click(screen.getByRole('button', { name: 'Eliminar actividad' }));

    expect(await screen.findByRole('button', { name: 'Eliminando…' })).toBeDisabled();
    finish();
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Eliminar actividad' })).toBeEnabled(),
    );
  });

  test('an activity that is already gone counts as deleted', async () => {
    vi.mocked(deleteActivity).mockRejectedValue(new ApiError(404));
    const { onDeleted } = renderModal();

    fireEvent.click(screen.getByRole('button', { name: 'Eliminar actividad' }));

    await waitFor(() => expect(onDeleted).toHaveBeenCalledTimes(1));
  });

  test.each([
    [new ApiError(401), 'Tu sesión ha caducado. Inicia sesión de nuevo.'],
    [new ApiError(403), 'No puedes eliminar esta actividad.'],
    [new NetworkError(), 'No se pudo conectar con el servidor. Inténtalo de nuevo.'],
    [new ApiError(500), 'No se pudo eliminar la actividad. Inténtalo de nuevo.'],
  ])('shows the failure inside the modal and keeps it open (%#)', async (failure, message) => {
    vi.mocked(deleteActivity).mockRejectedValue(failure);
    const { onDeleted } = renderModal();

    fireEvent.click(screen.getByRole('button', { name: 'Eliminar actividad' }));

    expect(await screen.findByText(message)).toBeInTheDocument();
    expect(onDeleted).not.toHaveBeenCalled();
    expect(screen.getByRole('dialog', { name: 'Eliminar actividad' })).toBeInTheDocument();
  });

  test('cancelling clears the error and does not delete anything', async () => {
    vi.mocked(deleteActivity).mockRejectedValue(new ApiError(500));
    const { onCancel } = renderModal();
    fireEvent.click(screen.getByRole('button', { name: 'Eliminar actividad' }));
    await screen.findByText('No se pudo eliminar la actividad. Inténtalo de nuevo.');

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(onCancel).toHaveBeenCalledTimes(1);
  });
});
