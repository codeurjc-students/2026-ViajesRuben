import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { getTripCost } from '../services/activityService';
import type { TripCost } from '../types';
import { useTripCost } from './useTripCost';

vi.mock('../services/activityService', () => ({ getTripCost: vi.fn() }));

const cost: TripCost = { totalCost: 120, remainingBudget: 80 };

beforeEach(() => {
  vi.mocked(getTripCost).mockReset();
});

describe('B6 useTripCost', () => {
  test('loads the cost of the trip', async () => {
    vi.mocked(getTripCost).mockResolvedValue(cost);

    const { result } = renderHook(() => useTripCost(7));

    expect(result.current.cost).toBeUndefined();
    await waitFor(() => expect(result.current.cost).toEqual(cost));
    expect(getTripCost).toHaveBeenCalledWith(7);
  });

  test('does not ask for a trip that is not loaded yet', () => {
    const { result } = renderHook(() => useTripCost(undefined));

    expect(result.current.cost).toBeUndefined();
    expect(getTripCost).not.toHaveBeenCalled();
  });

  test('a failure shows nothing and does not throw', async () => {
    vi.mocked(getTripCost).mockRejectedValue(new Error('down'));

    const { result } = renderHook(() => useTripCost(7));

    await waitFor(() => expect(getTripCost).toHaveBeenCalled());
    expect(result.current.cost).toBeUndefined();
  });

  test('reload asks again and keeps the old figure until the new one arrives', async () => {
    vi.mocked(getTripCost).mockResolvedValueOnce(cost);
    const { result } = renderHook(() => useTripCost(7));
    await waitFor(() => expect(result.current.cost).toEqual(cost));
    let answer: (value: TripCost) => void = () => {};
    vi.mocked(getTripCost).mockReturnValueOnce(
      new Promise((resolve) => {
        answer = resolve;
      }),
    );

    act(() => result.current.reload());

    await waitFor(() => expect(getTripCost).toHaveBeenCalledTimes(2));
    expect(result.current.cost).toEqual(cost);
    act(() => answer({ totalCost: 0, remainingBudget: 200 }));
    await waitFor(() =>
      expect(result.current.cost).toEqual({ totalCost: 0, remainingBudget: 200 }),
    );
  });

  test('never shows the figure of the previous trip for another id', async () => {
    vi.mocked(getTripCost).mockResolvedValue(cost);
    const { result, rerender } = renderHook(({ id }) => useTripCost(id), {
      initialProps: { id: 1 },
    });
    await waitFor(() => expect(result.current.cost).toEqual(cost));
    vi.mocked(getTripCost).mockReturnValue(new Promise(() => {}));

    rerender({ id: 2 });

    expect(result.current.cost).toBeUndefined();
  });

  test('ignores an answer that arrives after the id changed', async () => {
    let answerFirst: (value: TripCost) => void = () => {};
    vi.mocked(getTripCost).mockImplementation((id) =>
      id === 1
        ? new Promise((resolve) => {
            answerFirst = resolve;
          })
        : Promise.resolve({ totalCost: 5, remainingBudget: null }),
    );
    const { result, rerender } = renderHook(({ id }) => useTripCost(id), {
      initialProps: { id: 1 },
    });

    rerender({ id: 2 });
    await waitFor(() =>
      expect(result.current.cost).toEqual({ totalCost: 5, remainingBudget: null }),
    );
    act(() => answerFirst(cost));

    expect(result.current.cost).toEqual({ totalCost: 5, remainingBudget: null });
  });
});
