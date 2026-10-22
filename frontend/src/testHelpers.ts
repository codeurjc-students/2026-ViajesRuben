import type { Page, Trip } from './types';

/** Builds a page of the API for tests. */
export function pageOf<T>(content: T[], overrides: Partial<Page<T>> = {}): Page<T> {
  return {
    content,
    page: 0,
    size: 10,
    totalElements: content.length,
    last: true,
    ...overrides,
  };
}

/** Builds `count` trips whose ids start at `firstId`. */
export function tripsFrom(firstId: number, count: number): Trip[] {
  return Array.from({ length: count }, (_, i) => ({
    id: firstId + i,
    name: `Trip ${firstId + i}`,
    destination: `City ${firstId + i}`,
    startDate: '2027-05-01',
    endDate: '2027-05-05',
  }));
}
