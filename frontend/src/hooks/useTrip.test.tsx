import { renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { ApiError } from '../services/apiError';
import { getTrip } from '../services/tripService';
import { tripOf } from '../testHelpers';
import { useTrip } from './useTrip';

vi.mock('../services/tripService', () => ({ getTrip: vi.fn() }));

beforeEach(() => {
  vi.mocked(getTrip).mockReset();
});

describe('B4 useTrip', () => {
  test('loads the trip of the id', async () => {
    const trip = tripOf({ id: 7 });
    vi.mocked(getTrip).mockResolvedValue(trip);

    const { result } = renderHook(() => useTrip('7'));

    expect(result.current).toEqual({});
    await waitFor(() => expect(result.current.trip).toEqual(trip));
    expect(getTrip).toHaveBeenCalledWith(7);
  });

  test('exposes the error when the trip cannot be loaded', async () => {
    const failure = new ApiError(403);
    vi.mocked(getTrip).mockRejectedValue(failure);

    const { result } = renderHook(() => useTrip('7'));

    await waitFor(() => expect(result.current.error).toBe(failure));
    expect(result.current.trip).toBeUndefined();
  });

  test.each([undefined, 'abc', '1.5', ''])('"%s" is a trip that does not exist', (id) => {
    const { result } = renderHook(() => useTrip(id));

    expect(result.current.error).toMatchObject({ status: 404 });
    expect(getTrip).not.toHaveBeenCalled();
  });

  test('never shows the previous trip for another id', async () => {
    vi.mocked(getTrip).mockImplementation((id) => Promise.resolve(tripOf({ id })));
    const { result, rerender } = renderHook(({ id }) => useTrip(id), {
      initialProps: { id: '1' },
    });
    await waitFor(() => expect(result.current.trip?.id).toBe(1));

    rerender({ id: '2' });

    expect(result.current.trip).toBeUndefined();
    await waitFor(() => expect(result.current.trip?.id).toBe(2));
  });

  test('ignores an answer that arrives after the id changed', async () => {
    let answerFirst: (trip: ReturnType<typeof tripOf>) => void = () => {};
    vi.mocked(getTrip).mockImplementation((id) =>
      id === 1
        ? new Promise((resolve) => {
            answerFirst = resolve;
          })
        : Promise.resolve(tripOf({ id })),
    );
    const { result, rerender } = renderHook(({ id }) => useTrip(id), {
      initialProps: { id: '1' },
    });

    rerender({ id: '2' });
    await waitFor(() => expect(result.current.trip?.id).toBe(2));
    answerFirst(tripOf({ id: 1 }));

    await waitFor(() => expect(result.current.trip?.id).toBe(2));
  });
});
