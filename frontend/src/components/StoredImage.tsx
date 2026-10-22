import { useState } from 'react';
import { DEFAULT_TRIP_COVER } from '../constants/images';
import { imageUrl } from '../services/imageService';

interface StoredImageProps {
  /** Id of an image stored in the backend; without it the default cover is shown. */
  id?: string;
  alt?: string;
  className?: string;
}

/** Shows a stored image by id, falling back to the default cover if it is missing or fails to load. */
export default function StoredImage({ id, alt = '', className }: StoredImageProps) {
  const [failedId, setFailedId] = useState<string>();
  const src = id && id !== failedId ? imageUrl(id) : DEFAULT_TRIP_COVER;

  return <img src={src} alt={alt} className={className} onError={() => setFailedId(id)} />;
}
