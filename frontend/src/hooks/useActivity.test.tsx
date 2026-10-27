import { renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { ApiError } from '../services/apiError';
import { getActivity } from '../services/activityService';
import { activityOf } from '../testHelpers';
import { useActivity } from './useActivity';

vi.mock('../services/activityService', () => ({ getActivity: vi.fn() }));

beforeEach(() => {
  vi.mocked(getActivity).mockReset();
});

describe('B5 useActivity', () => {
  test('loads the activity of the ids', async () => {
    const activity = activityOf({ id: 7, tripId: 3 });
    vi.mocked(getActivity).mockResolvedValue(activity);

    const { result } = renderHook(() => useActivity('3', '7'));

    expect(result.current).toEqual({});
    await waitFor(() => expect(result.current.activity).toEqual(activity));
    expect(getActivity).toHaveBeenCalledWith(3, 7);
  });

  test('loads nothing when there is no activity id, as in the form that creates', () => {
    const { result } = renderHook(() => useActivity('3', undefined));

    expect(result.current).toEqual({});
    expect(getActivity).not.toHaveBeenCalled();
  });

  test('exposes the error when the activity cannot be loaded', async () => {
    const failure = new ApiError(404);
    vi.mocked(getActivity).mockRejectedValue(failure);

    const { result } = renderHook(() => useActivity('3', '7'));

    await waitFor(() => expect(result.current.error).toBe(failure));
    expect(result.current.activity).toBeUndefined();
  });

  test.each([
    ['abc', '7'],
    ['3', 'abc'],
    ['3', '1.5'],
    ['', '7'],
    [undefined, '7'],
  ])('trip "%s" and activity "%s" are an activity that does not exist', (tripId, activityId) => {
    const { result } = renderHook(() => useActivity(tripId, activityId));

    expect(result.current.error).toMatchObject({ status: 404 });
    expect(getActivity).not.toHaveBeenCalled();
  });

  test('never shows the previous activity for another id', async () => {
    vi.mocked(getActivity).mockImplementation((tripId, id) =>
      Promise.resolve(activityOf({ id, tripId })),
    );
    const { result, rerender } = renderHook(({ id }) => useActivity('3', id), {
      initialProps: { id: '1' },
    });
    await waitFor(() => expect(result.current.activity?.id).toBe(1));

    rerender({ id: '2' });

    expect(result.current.activity).toBeUndefined();
    await waitFor(() => expect(result.current.activity?.id).toBe(2));
  });

  test('ignores an answer that arrives after the id changed', async () => {
    let answerFirst: (activity: ReturnType<typeof activityOf>) => void = () => {};
    vi.mocked(getActivity).mockImplementation((tripId, id) =>
      id === 1
        ? new Promise((resolve) => {
            answerFirst = resolve;
          })
        : Promise.resolve(activityOf({ id, tripId })),
    );
    const { result, rerender } = renderHook(({ id }) => useActivity('3', id), {
      initialProps: { id: '1' },
    });

    rerender({ id: '2' });
    await waitFor(() => expect(result.current.activity?.id).toBe(2));
    answerFirst(activityOf({ id: 1 }));

    await waitFor(() => expect(result.current.activity?.id).toBe(2));
  });
});
