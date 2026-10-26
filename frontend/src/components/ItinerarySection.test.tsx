import { fireEvent, screen, within } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { renderRoutes } from '../renderHelpers';
import { ApiError } from '../services/apiError';
import { getActivities } from '../services/activityService';
import { activityOf, ANA, pageOf, tripOf } from '../testHelpers';
import ItinerarySection from './ItinerarySection';

vi.mock('../services/activityService', () => ({ getActivities: vi.fn() }));

const trip = tripOf({ id: 3, creatorId: ANA.id, startDate: '2027-05-01', endDate: '2027-05-04' });

const routes = [
  { path: '/trips/:tripId', element: <ItinerarySection trip={trip} />, errorElement: <p>Error</p> },
];

function activitiesOfDay(day: string) {
  return day === '2027-05-02'
    ? [
        activityOf({ id: 1, name: 'Museo', date: day, startTime: '10:00:00', endTime: '11:00:00' }),
        activityOf({ id: 2, name: 'Cena', date: day, startTime: '21:00:00' }),
      ]
    : [];
}

beforeEach(() => {
  vi.mocked(getActivities)
    .mockReset()
    .mockImplementation((_tripId, day) => Promise.resolve(pageOf(activitiesOfDay(day ?? ''))));
});

describe('B5 ItinerarySection', () => {
  test('has one tab per day of the trip and starts on the first day', async () => {
    renderRoutes(routes, '/trips/3');

    const tabs = screen.getAllByRole('tab');
    expect(tabs).toHaveLength(4);
    expect(tabs[0]).toHaveTextContent('Día 1');
    expect(tabs[3]).toHaveTextContent('Día 4');
    expect(tabs[0]).toHaveAttribute('aria-selected', 'true');
    expect(tabs[1]).toHaveAttribute('aria-selected', 'false');
    await screen.findByText('No hay actividades este día', { exact: false });
    expect(getActivities).toHaveBeenCalledWith(3, '2027-05-01', 0);
  });

  test('shows the activities of the chosen day in the order the server gives them', async () => {
    renderRoutes(routes, '/trips/3?day=2027-05-02');

    const items = await screen.findAllByRole('listitem');
    expect(items).toHaveLength(2);
    expect(within(items[0]).getByRole('heading', { name: 'Museo' })).toBeInTheDocument();
    expect(within(items[1]).getByRole('heading', { name: 'Cena' })).toBeInTheDocument();
    expect(screen.getByText('2 actividades')).toBeInTheDocument();
    expect(screen.getByRole('tab', { selected: true })).toHaveTextContent('Día 2');
  });

  test('clicking a day loads that day and keeps it in the URL', async () => {
    const router = renderRoutes(routes, '/trips/3');
    await screen.findByText('No hay actividades este día', { exact: false });

    fireEvent.click(screen.getByRole('tab', { name: /Día 2/ }));

    expect(await screen.findByRole('heading', { name: 'Museo' })).toBeInTheDocument();
    expect(router.state.location.search).toBe('?day=2027-05-02');
    expect(getActivities).toHaveBeenCalledWith(3, '2027-05-02', 0);
    expect(
      screen.queryByText('No hay actividades este día', { exact: false }),
    ).not.toBeInTheDocument();
  });

  test('previous and next move one day, and stop at the ends', async () => {
    renderRoutes(routes, '/trips/3');
    const previous = screen.getByRole('button', { name: 'Día anterior' });
    const next = screen.getByRole('button', { name: 'Día siguiente' });
    expect(previous).toBeDisabled();

    fireEvent.click(next);
    expect(await screen.findByRole('heading', { name: 'Museo' })).toBeInTheDocument();
    expect(previous).toBeEnabled();
    fireEvent.click(previous);
    expect(screen.getByRole('tab', { selected: true })).toHaveTextContent('Día 1');
    fireEvent.click(screen.getByRole('tab', { name: /Día 4/ }));

    expect(next).toBeDisabled();
    await screen.findByText('No hay actividades este día', { exact: false });
  });

  test('a day that is not in the URL or not in the trip falls back to the first day', async () => {
    renderRoutes(routes, '/trips/3?day=2030-01-01');

    expect(screen.getByRole('tab', { selected: true })).toHaveTextContent('Día 1');
    await screen.findByText('No hay actividades este día', { exact: false });
    expect(getActivities).toHaveBeenCalledWith(3, '2027-05-01', 0);
  });

  test('an empty day invites the creator to add the first activity', async () => {
    renderRoutes(routes, '/trips/3');

    expect(
      await screen.findByText(
        'No hay actividades este día. Añade la primera con «Nueva actividad».',
      ),
    ).toBeInTheDocument();
  });

  test('"Nueva actividad" opens the form on the selected day', async () => {
    renderRoutes(routes, '/trips/3?day=2027-05-03');
    await screen.findByText('No hay actividades este día', { exact: false });

    expect(screen.getByRole('link', { name: 'Nueva actividad' })).toHaveAttribute(
      'href',
      '/trips/3/activities/new?day=2027-05-03',
    );
  });

  test('anyone else sees the itinerary without buttons to change it', async () => {
    renderRoutes(routes, '/trips/3?day=2027-05-02', { ...ANA, id: ANA.id + 1 });

    await screen.findByRole('heading', { name: 'Museo' });

    expect(screen.queryByRole('link', { name: 'Nueva actividad' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Editar Museo' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Eliminar Museo' })).not.toBeInTheDocument();
  });

  test('shows a single activity as "1 actividad"', async () => {
    vi.mocked(getActivities).mockResolvedValue(pageOf([activityOf({ name: 'Museo' })]));
    renderRoutes(routes, '/trips/3');

    expect(await screen.findByText('1 actividad')).toBeInTheDocument();
  });

  test('loads the next page of a long day with "Más resultados"', async () => {
    vi.mocked(getActivities).mockImplementation((_tripId, _day, page = 0) =>
      Promise.resolve(
        page === 0
          ? pageOf([activityOf({ id: 1, name: 'Primera' })], { last: false, totalElements: 2 })
          : pageOf([activityOf({ id: 2, name: 'Segunda' })], { page: 1, totalElements: 2 }),
      ),
    );
    renderRoutes(routes, '/trips/3');
    await screen.findByRole('heading', { name: 'Primera' });

    fireEvent.click(screen.getByRole('button', { name: 'Más resultados' }));

    expect(await screen.findByRole('heading', { name: 'Segunda' })).toBeInTheDocument();
    expect(getActivities).toHaveBeenLastCalledWith(3, '2027-05-01', 1);
    expect(screen.queryByRole('button', { name: 'Más resultados' })).not.toBeInTheDocument();
  });

  test('shows a loading text until the first page arrives', async () => {
    renderRoutes(routes, '/trips/3');

    expect(screen.getByRole('status')).toHaveTextContent('Cargando actividades…');
    await screen.findByText('No hay actividades este día', { exact: false });
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  test('a failure loading the activities goes to the route error page', async () => {
    vi.mocked(getActivities).mockRejectedValue(new ApiError(500));
    renderRoutes(routes, '/trips/3');

    expect(await screen.findByText('Error')).toBeInTheDocument();
  });

  test('a one-day trip has a single tab and no day to move to', async () => {
    const oneDay = tripOf({ id: 3, startDate: '2027-05-01', endDate: '2027-05-01' });
    renderRoutes(
      [{ path: '/trips/:tripId', element: <ItinerarySection trip={oneDay} /> }],
      '/trips/3',
    );

    expect(screen.getAllByRole('tab')).toHaveLength(1);
    expect(screen.getByRole('button', { name: 'Día anterior' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Día siguiente' })).toBeDisabled();
    await screen.findByText('No hay actividades este día', { exact: false });
  });
});
