import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { renderAsUser } from '../renderHelpers';
import { ApiError } from '../services/apiError';
import { getActivityLog } from '../services/activityLogService';
import { logEntryOf, pageOf } from '../testHelpers';
import type { ActivityLogEntry } from '../types';
import ActivityLogSection from './ActivityLogSection';

vi.mock('../services/activityLogService', () => ({ getActivityLog: vi.fn() }));

beforeEach(() => {
  vi.mocked(getActivityLog).mockReset().mockResolvedValue(pageOf([]));
});

/** `count` entries, newest first, whose ids and names start at `first`. */
function entries(count: number, first = 1): ActivityLogEntry[] {
  return Array.from({ length: count }, (_, i) =>
    logEntryOf({ id: first + i, activityName: `Actividad ${first + i}` }),
  );
}

/** The log of a trip in pages of 10, like the server's: `total` entries in all. */
function serverWith(total: number) {
  vi.mocked(getActivityLog).mockImplementation((_tripId, page = 0) => {
    const first = page * 10;
    const count = Math.max(0, Math.min(10, total - first));
    return Promise.resolve(
      pageOf(entries(count, first + 1), {
        page,
        totalElements: total,
        last: first + count >= total,
      }),
    );
  });
}

function shown() {
  return screen.queryAllByRole('listitem');
}

describe('I4 ActivityLogSection', () => {
  test('shows a loading text and then asks for the first page of the trip', async () => {
    renderAsUser(<ActivityLogSection tripId={3} />);

    expect(screen.getByRole('status')).toHaveTextContent('Cargando historial…');
    await screen.findByText('Aún no hay cambios');
    expect(getActivityLog).toHaveBeenCalledWith(3, 0);
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  test('has a heading of its own', async () => {
    renderAsUser(<ActivityLogSection tripId={3} />);

    expect(screen.getByRole('heading', { name: 'Historial de cambios' })).toBeInTheDocument();
    await screen.findByText('Aún no hay cambios');
  });

  test('without entries it says so in one short line and offers no buttons', async () => {
    renderAsUser(<ActivityLogSection tripId={3} />);

    expect(await screen.findByText('Aún no hay cambios')).toBeInTheDocument();
    expect(shown()).toHaveLength(0);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  test('one short line per entry: who, what they did, the activity and how long ago', async () => {
    vi.mocked(getActivityLog).mockResolvedValue(
      pageOf([
        logEntryOf({
          id: 3,
          userName: 'Laura',
          action: 'UPDATED',
          activityName: 'Visita al museo',
          changedFields: [],
          createdAt: new Date(Date.now() - 2 * 3600_000 - 5000).toISOString(),
        }),
        logEntryOf({ id: 2, userName: 'Bob', action: 'CREATED', activityName: 'Cena' }),
        logEntryOf({
          id: 1,
          userName: 'Ana',
          action: 'DELETED',
          activityId: null,
          activityName: 'Paseo',
          createdAt: new Date(Date.now() - 3 * 24 * 3600_000 - 5000).toISOString(),
        }),
      ]),
    );
    renderAsUser(<ActivityLogSection tripId={3} />);

    const lines = await screen.findAllByRole('listitem');

    expect(lines[0]).toHaveTextContent('Laura editó «Visita al museo» · hace 2 h');
    expect(lines[1]).toHaveTextContent('Bob añadió «Cena» · ahora');
    expect(lines[2]).toHaveTextContent('Ana eliminó «Paseo» · hace 3 d');
  });

  test('the changed fields go in a smaller second line of an edit, with their Spanish names', async () => {
    vi.mocked(getActivityLog).mockResolvedValue(
      pageOf([
        logEntryOf({
          id: 1,
          action: 'UPDATED',
          changedFields: ['name', 'startTime', 'endTime', 'cost', 'image'],
        }),
      ]),
    );
    renderAsUser(<ActivityLogSection tripId={3} />);

    const line = await screen.findByRole('listitem');

    expect(
      within(line).getByText('Cambios: nombre, hora de inicio, hora de fin, coste, imagen'),
    ).toBeInTheDocument();
    // The main line does not carry them.
    expect(line.querySelector('.activity-log__line')).not.toHaveTextContent('nombre');
    expect(line.querySelector('.activity-log__line')).toHaveTextContent(/^Bob editó «Museo»/);
  });

  test('every editable field has a Spanish name and an unknown one is shown as it comes', async () => {
    vi.mocked(getActivityLog).mockResolvedValue(
      pageOf([
        logEntryOf({
          action: 'UPDATED',
          changedFields: ['description', 'location', 'date', 'category', 'weather'],
        }),
      ]),
    );
    renderAsUser(<ActivityLogSection tripId={3} />);

    expect(
      await screen.findByText('Cambios: descripción, lugar, fecha, categoría, weather'),
    ).toBeInTheDocument();
  });

  test('only an edit has a second line', async () => {
    vi.mocked(getActivityLog).mockResolvedValue(
      pageOf([
        logEntryOf({ id: 3, action: 'CREATED' }),
        logEntryOf({ id: 2, action: 'DELETED', activityId: null }),
        logEntryOf({ id: 1, action: 'UPDATED', changedFields: [] }),
      ]),
    );
    renderAsUser(<ActivityLogSection tripId={3} />);

    await screen.findAllByRole('listitem');

    expect(screen.queryByText(/^Cambios:/)).not.toBeInTheDocument();
  });

  test('the time says exactly when in its tooltip and keeps the instant for machines', async () => {
    const createdAt = '2027-07-10T12:30:00Z';
    vi.mocked(getActivityLog).mockResolvedValue(pageOf([logEntryOf({ createdAt })]));
    renderAsUser(<ActivityLogSection tripId={3} />);

    const time = (await screen.findByRole('listitem')).querySelector('time');

    expect(time).toHaveAttribute('datetime', createdAt);
    expect(time?.getAttribute('title')).toMatch(/^10 jul 2027,? \d{1,2}:\d{2}$/);
  });

  test('at first it shows only the 5 latest entries and offers "Ver más"', async () => {
    serverWith(10);
    renderAsUser(<ActivityLogSection tripId={3} />);

    await screen.findAllByRole('listitem');

    expect(shown()).toHaveLength(5);
    expect(shown()[0]).toHaveTextContent('Actividad 1');
    expect(shown()[4]).toHaveTextContent('Actividad 5');
    expect(screen.getByRole('button', { name: 'Ver más' })).toBeEnabled();
    expect(screen.queryByRole('button', { name: 'Ver menos' })).not.toBeInTheDocument();
  });

  test('exactly 5 entries show with no buttons', async () => {
    serverWith(5);
    renderAsUser(<ActivityLogSection tripId={3} />);

    await screen.findAllByRole('listitem');

    expect(shown()).toHaveLength(5);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  test('"Ver más" shows the entries already loaded without asking the server again', async () => {
    serverWith(8);
    renderAsUser(<ActivityLogSection tripId={3} />);
    await screen.findAllByRole('listitem');

    fireEvent.click(screen.getByRole('button', { name: 'Ver más' }));

    expect(shown()).toHaveLength(8);
    expect(getActivityLog).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole('button', { name: 'Ver más' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ver menos' })).toBeInTheDocument();
  });

  test('"Ver más" loads the next page, 10 more at a time, until there is nothing left', async () => {
    serverWith(25);
    renderAsUser(<ActivityLogSection tripId={3} />);
    await screen.findAllByRole('listitem');

    fireEvent.click(screen.getByRole('button', { name: 'Ver más' }));
    await waitFor(() => expect(shown()).toHaveLength(15));
    expect(getActivityLog).toHaveBeenLastCalledWith(3, 1);

    fireEvent.click(screen.getByRole('button', { name: 'Ver más' }));
    await waitFor(() => expect(shown()).toHaveLength(25));
    expect(getActivityLog).toHaveBeenLastCalledWith(3, 2);
    expect(getActivityLog).toHaveBeenCalledTimes(3);
    expect(screen.queryByRole('button', { name: 'Ver más' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ver menos' })).toBeInTheDocument();
  });

  test('"Ver menos" goes back to 5 and "Ver más" shows 10 more again without asking the server', async () => {
    serverWith(25);
    renderAsUser(<ActivityLogSection tripId={3} />);
    await screen.findAllByRole('listitem');
    fireEvent.click(screen.getByRole('button', { name: 'Ver más' }));
    await waitFor(() => expect(shown()).toHaveLength(15));
    expect(screen.getByRole('button', { name: 'Ver menos' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ver más' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Ver menos' }));

    expect(shown()).toHaveLength(5);
    expect(screen.queryByRole('button', { name: 'Ver menos' })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Ver más' }));
    expect(shown()).toHaveLength(15);
    expect(getActivityLog).toHaveBeenCalledTimes(2);
  });

  test('"Ver más" is disabled while the next page is loading', async () => {
    serverWith(25);
    renderAsUser(<ActivityLogSection tripId={3} />);
    await screen.findAllByRole('listitem');
    vi.mocked(getActivityLog).mockReturnValue(new Promise(() => {}));

    fireEvent.click(screen.getByRole('button', { name: 'Ver más' }));

    await waitFor(() => expect(screen.getByRole('button', { name: 'Ver más' })).toBeDisabled());
  });

  test('a failure loading the log says so and does not break the page', async () => {
    vi.mocked(getActivityLog).mockRejectedValue(new ApiError(500));
    renderAsUser(<ActivityLogSection tripId={3} />);

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudo cargar el historial de cambios.',
    );
    expect(screen.getByRole('heading', { name: 'Historial de cambios' })).toBeInTheDocument();
    expect(screen.queryByText('Cargando historial…')).not.toBeInTheDocument();
  });
});
