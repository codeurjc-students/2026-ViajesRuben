import { useCallback, useEffect, useRef, useState } from 'react';
import type { Page } from '../types';

export interface PagedList<T> {
  items: T[];
  totalElements: number;
  /** True once the first page has arrived. */
  loaded: boolean;
  /** True while a page is being fetched. */
  loading: boolean;
  /** True when the server has no more pages. */
  last: boolean;
  error: unknown;
  /** Fetches the next page and appends it to `items`. */
  loadMore: () => void;
  /** Forgets everything and loads the first page again, e.g. after deleting an item. */
  reload: () => void;
}

/** Loads a paginated list one page at a time; `fetchPage` must keep the same identity between renders. */
export function usePagedList<T>(fetchPage: (page: number) => Promise<Page<T>>): PagedList<T> {
  const [items, setItems] = useState<T[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [loaded, setLoaded] = useState(false);
  const [loading, setLoading] = useState(true);
  const [last, setLast] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const nextPage = useRef(0);
  const fetching = useRef(false);
  /** Changes on every reload, so an answer to a request made before it is ignored. */
  const generation = useRef(0);

  const loadMore = useCallback(() => {
    if (fetching.current) return;
    fetching.current = true;
    setLoading(true);
    const mine = generation.current;
    void fetchPage(nextPage.current)
      .then((page) => {
        if (mine !== generation.current) return;
        setItems((current) => [...current, ...page.content]);
        setTotalElements(page.totalElements);
        setLast(page.last);
        setLoaded(true);
        nextPage.current = page.page + 1;
      })
      .catch((failure) => {
        if (mine === generation.current) setError(failure);
      })
      .finally(() => {
        if (mine !== generation.current) return;
        fetching.current = false;
        setLoading(false);
      });
  }, [fetchPage]);

  const reload = useCallback(() => {
    generation.current += 1;
    fetching.current = false;
    nextPage.current = 0;
    setItems([]);
    setTotalElements(0);
    setLoaded(false);
    setLast(false);
    setError(null);
    loadMore();
  }, [loadMore]);

  useEffect(() => {
    loadMore();
  }, [loadMore]);

  return { items, totalElements, loaded, loading, last, error, loadMore, reload };
}
