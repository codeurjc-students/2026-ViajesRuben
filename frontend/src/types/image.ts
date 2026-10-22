/** Mirrors the backend ImageDTO. The bytes are read from `/api/v1/images/{id}`. */
export interface ImageInfo {
  id: string;
  contentType: string;
  size: number;
}
