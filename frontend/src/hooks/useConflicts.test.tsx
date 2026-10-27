import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { getConflicts } from '../services/conflictService';
import { conflictsOf } from '../testHelpers';
import { useConflicts } from './useConflicts';

vi.mock('../services/conflictService', () => ({ getConflicts: vi.fn() }));

const budgetConflict = conflictsOf({ budget: { totalCost: 120, budget: 100 } });

beforeEach(() => {
  vi.mocked(getConflicts).mockReset();
});

describe('B6 useConflicts', () => {
  test('loads the conflicts of the trip', async () => {
    vi.mocked(getConflicts).mockResolvedValue(budgetConflict);

    const { result } = renderHook(() => useConflicts(7));

    expect(result.current.conflicts).toBeUndefined();
    await waitFor(() => expect(result.current.conflicts).toEqual(budgetConflict));
    expect(getConflicts).toHaveBeenCalledWith(7);
  });

  test('does not ask for a trip that is not loaded yet', () => {
    const { result } = renderHook(() => useConflicts(undefined));

    expect(result.current.conflicts).toBeUndefined();
    expect(getConflicts).not.toHaveBeenCalled();
  });

  test('a failure shows nothing and does not throw', async () => {
    vi.mocked(getConflicts).mockRejectedValue(new Error('down'));

    const { result } = renderHook(() => useConflicts(7));

    await waitFor(() => expect(getConflicts).toHaveBeenCalled());
    expect(result.current.conflicts).toBeUndefined();
  });

  test('reload asks again and keeps the old answer until the new one arrives', async () => {
    vi.mocked(getConflicts).mockResolvedValueOnce(budgetConflict);
    const { result } = renderHook(() => useConflicts(7));
    await waitFor(() => expect(result.current.conflicts).toEqual(budgetConflict));
    let answer: (value: ReturnType<typeof conflictsOf>) => void = () => {};
    vi.mocked(getConflicts).mockReturnValueOnce(
      new Promise((resolve) => {
        answer = resolve;
      }),
    );

    act(() => result.current.reload());

    await waitFor(() => expect(getConflicts).toHaveBeenCalledTimes(2));
    expect(result.current.conflicts).toEqual(budgetConflict);
    act(() => answer(conflictsOf()));
    await waitFor(() => expect(result.current.conflicts).toEqual(conflictsOf()));
  });

  test('never shows the conflicts of the previous trip for another id', async () => {
    vi.mocked(getConflicts).mockResolvedValue(budgetConflict);
    const { result, rerender } = renderHook(({ id }) => useConflicts(id), {
      initialProps: { id: 1 },
    });
    await waitFor(() => expect(result.current.conflicts).toEqual(budgetConflict));
    vi.mocked(getConflicts).mockReturnValue(new Promise(() => {}));

    rerender({ id: 2 });

    expect(result.current.conflicts).toBeUndefined();
  });

  test('ignores an answer that arrives after the id changed', async () => {
    let answerFirst: (value: ReturnType<typeof conflictsOf>) => void = () => {};
    vi.mocked(getConflicts).mockImplementation((id) =>
      id === 1
        ? new Promise((resolve) => {
            answerFirst = resolve;
          })
        : Promise.resolve(conflictsOf()),
    );
    const { result, rerender } = renderHook(({ id }) => useConflicts(id), {
      initialProps: { id: 1 },
    });

    rerender({ id: 2 });
    await waitFor(() => expect(result.current.conflicts).toEqual(conflictsOf()));
    act(() => answerFirst(budgetConflict));

    expect(result.current.conflicts).toEqual(conflictsOf());
  });
});
