/** Mirrors the backend PageDTO: one page of a paginated list. */
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  last: boolean;
}
