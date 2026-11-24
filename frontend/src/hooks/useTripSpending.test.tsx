import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { getTripSpending } from '../services/activityService';
import { spendingOf } from '../testHelpers';
import { useTripSpending } from './useTripSpending';

vi.mock('../services/activityService', () => ({ getTripSpending: vi.fn() }));

const spending = spendingOf({ budget: 100, totalCost: 40 });

beforeEach(() => {
  vi.mocked(getTripSpending).mockReset();
});

describe('I5 useTripSpending', () => {
  test('loads the spending of the trip', async () => {
    vi.mocked(getTripSpending).mockResolvedValue(spending);

    const { result } = renderHook(() => useTripSpending(7));

    expect(result.current.spending).toBeUndefined();
    await waitFor(() => expect(result.current.spending).toEqual(spending));
    expect(result.current.failed).toBe(false);
    expect(getTripSpending).toHaveBeenCalledWith(7);
  });

  test('does not ask for a trip that is not loaded yet, or one that is not for this user', () => {
    const { result } = renderHook(() => useTripSpending(undefined));

    expect(result.current.spending).toBeUndefined();
    expect(result.current.failed).toBe(false);
    expect(getTripSpending).not.toHaveBeenCalled();
  });

  test('a failure is reported and does not throw', async () => {
    vi.mocked(getTripSpending).mockRejectedValue(new Error('down'));

    const { result } = renderHook(() => useTripSpending(7));

    await waitFor(() => expect(result.current.failed).toBe(true));
    expect(result.current.spending).toBeUndefined();
  });

  test('reload asks again, keeps the old data until the answer and clears an old failure', async () => {
    const newer = spendingOf({ budget: 100, totalCost: 10 });
    vi.mocked(getTripSpending).mockResolvedValueOnce(spending);
    const { result } = renderHook(() => useTripSpending(7));
    await waitFor(() => expect(result.current.spending).toEqual(spending));

    let answer: (value: typeof newer) => void = () => {};
    vi.mocked(getTripSpending).mockReturnValueOnce(new Promise((resolve) => (answer = resolve)));
    act(() => result.current.reload());

    expect(result.current.spending).toEqual(spending);
    await act(async () => answer(newer));
    expect(result.current.spending).toEqual(newer);
    expect(getTripSpending).toHaveBeenCalledTimes(2);
  });

  test('a failed reload leaves the data it already had and says it failed', async () => {
    vi.mocked(getTripSpending).mockResolvedValueOnce(spending);
    const { result } = renderHook(() => useTripSpending(7));
    await waitFor(() => expect(result.current.spending).toEqual(spending));

    vi.mocked(getTripSpending).mockRejectedValueOnce(new Error('down'));
    act(() => result.current.reload());

    await waitFor(() => expect(result.current.failed).toBe(true));
    expect(result.current.spending).toEqual(spending);
  });

  test('never shows the figures of another trip', async () => {
    vi.mocked(getTripSpending).mockImplementation((id) =>
      id === 1 ? Promise.resolve(spending) : new Promise(() => {}),
    );
    const { result, rerender } = renderHook(({ id }) => useTripSpending(id), {
      initialProps: { id: 1 },
    });
    await waitFor(() => expect(result.current.spending).toEqual(spending));

    rerender({ id: 2 });

    expect(result.current.spending).toBeUndefined();
  });
});
