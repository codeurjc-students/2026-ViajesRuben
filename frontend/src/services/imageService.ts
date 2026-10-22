import type { ImageInfo } from '../types';
import { API_URL, request } from './apiError';

/** Uploads a JPEG, PNG or WebP image; the server answers 400 for anything else. */
export function uploadImage(file: File): Promise<ImageInfo> {
  const body = new FormData();
  body.append('file', file);
  return request<ImageInfo>('/api/v1/images', { method: 'POST', body });
}

/** URL of the bytes of a stored image; it is public, so it works in an `<img>` tag. */
export function imageUrl(id: string): string {
  return `${API_URL}/api/v1/images/${id}`;
}
