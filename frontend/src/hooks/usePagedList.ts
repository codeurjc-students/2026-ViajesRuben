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

  const loadMore = useCallback(() => {
    if (fetching.current) return;
    fetching.current = true;
    setLoading(true);
    fetchPage(nextPage.current)
      .then((page) => {
        setItems((current) => [...current, ...page.content]);
        setTotalElements(page.totalElements);
        setLast(page.last);
        setLoaded(true);
        nextPage.current = page.page + 1;
      })
      .catch(setError)
      .finally(() => {
        fetching.current = false;
        setLoading(false);
      });
  }, [fetchPage]);

  useEffect(() => {
    loadMore();
  }, [loadMore]);

  return { items, totalElements, loaded, loading, last, error, loadMore };
}
