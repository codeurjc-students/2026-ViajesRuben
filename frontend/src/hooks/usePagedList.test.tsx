import { act, renderHook, waitFor } from '@testing-library/react';
import { expect, test, vi } from 'vitest';
import { pageOf } from '../testHelpers';
import { usePagedList } from './usePagedList';

test('loads the first page on mount', async () => {
  const fetchPage = vi.fn().mockResolvedValue(pageOf(['a', 'b'], { totalElements: 2 }));

  const { result } = renderHook(() => usePagedList<string>(fetchPage));

  expect(result.current.loaded).toBe(false);
  await waitFor(() => expect(result.current.loaded).toBe(true));
  expect(fetchPage).toHaveBeenCalledWith(0);
  expect(result.current.items).toEqual(['a', 'b']);
  expect(result.current.totalElements).toBe(2);
  expect(result.current.last).toBe(true);
  expect(result.current.loading).toBe(false);
});

test('loadMore appends the next page', async () => {
  const fetchPage = vi
    .fn()
    .mockResolvedValueOnce(pageOf(['a', 'b'], { page: 0, totalElements: 3, last: false }))
    .mockResolvedValueOnce(pageOf(['c'], { page: 1, totalElements: 3, last: true }));
  const { result } = renderHook(() => usePagedList<string>(fetchPage));
  await waitFor(() => expect(result.current.loaded).toBe(true));
  expect(result.current.last).toBe(false);

  act(() => result.current.loadMore());

  await waitFor(() => expect(result.current.items).toEqual(['a', 'b', 'c']));
  expect(fetchPage).toHaveBeenLastCalledWith(1);
  expect(result.current.last).toBe(true);
});

test('ignores loadMore while a page is already loading', async () => {
  let resolve: (value: ReturnType<typeof pageOf<string>>) => void = () => {};
  const fetchPage = vi.fn().mockReturnValue(
    new Promise((r) => {
      resolve = r;
    }),
  );
  const { result } = renderHook(() => usePagedList<string>(fetchPage));

  act(() => result.current.loadMore());
  act(() => result.current.loadMore());
  expect(fetchPage).toHaveBeenCalledTimes(1);

  await act(async () => resolve(pageOf(['a'])));
  expect(result.current.items).toEqual(['a']);
});

test('exposes the error when a page cannot be loaded', async () => {
  const failure = new Error('boom');
  const fetchPage = vi.fn().mockRejectedValue(failure);

  const { result } = renderHook(() => usePagedList<string>(fetchPage));

  await waitFor(() => expect(result.current.error).toBe(failure));
  expect(result.current.loaded).toBe(false);
  expect(result.current.loading).toBe(false);
});

test('B4 reload forgets the items and loads the first page again', async () => {
  const fetchPage = vi
    .fn()
    .mockResolvedValueOnce(pageOf(['a', 'b'], { page: 0, totalElements: 3, last: false }))
    .mockResolvedValueOnce(pageOf(['c'], { page: 1, totalElements: 3, last: true }))
    .mockResolvedValueOnce(pageOf(['b', 'c'], { page: 0, totalElements: 2, last: true }));
  const { result } = renderHook(() => usePagedList<string>(fetchPage));
  await waitFor(() => expect(result.current.loaded).toBe(true));
  act(() => result.current.loadMore());
  await waitFor(() => expect(result.current.items).toEqual(['a', 'b', 'c']));

  act(() => result.current.reload());

  expect(result.current.items).toEqual([]);
  await waitFor(() => expect(result.current.items).toEqual(['b', 'c']));
  expect(fetchPage).toHaveBeenLastCalledWith(0);
  expect(result.current.totalElements).toBe(2);
  expect(result.current.last).toBe(true);
});

test('B4 an answer to a request made before a reload is ignored', async () => {
  let resolveOld: (value: ReturnType<typeof pageOf<string>>) => void = () => {};
  const fetchPage = vi
    .fn()
    .mockReturnValueOnce(
      new Promise((r) => {
        resolveOld = r;
      }),
    )
    .mockResolvedValueOnce(pageOf(['new']));
  const { result } = renderHook(() => usePagedList<string>(fetchPage));

  act(() => result.current.reload());
  await waitFor(() => expect(result.current.items).toEqual(['new']));
  await act(async () => resolveOld(pageOf(['old'])));

  expect(result.current.items).toEqual(['new']);
  expect(result.current.loading).toBe(false);
});

test('B4 reload clears a previous error', async () => {
  const fetchPage = vi
    .fn()
    .mockRejectedValueOnce(new Error('boom'))
    .mockResolvedValueOnce(pageOf(['a']));
  const { result } = renderHook(() => usePagedList<string>(fetchPage));
  await waitFor(() => expect(result.current.error).toBeInstanceOf(Error));

  act(() => result.current.reload());

  await waitFor(() => expect(result.current.items).toEqual(['a']));
  expect(result.current.error).toBeNull();
});
