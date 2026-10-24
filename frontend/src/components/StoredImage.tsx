import { useState, type ReactNode } from 'react';
import { DEFAULT_TRIP_COVER } from '../constants/images';
import { imageUrl } from '../services/imageService';

interface StoredImageProps {
  /** Id of an image stored in the backend; without it the fallback is shown. */
  id?: string;
  alt?: string;
  className?: string;
  /** Shown instead of the default cover when there is no id or the image fails to load. */
  fallback?: ReactNode;
}

/** Shows a stored image by id, falling back to `fallback` (or the default cover) if it is missing or fails to load. */
export default function StoredImage({
  id,
  alt = '',
  className,
  fallback,
}: Readonly<StoredImageProps>) {
  const [failedId, setFailedId] = useState<string>();
  const available = id !== undefined && id !== failedId;

  if (!available && fallback !== undefined) return <>{fallback}</>;
  const src = available ? imageUrl(id) : DEFAULT_TRIP_COVER;
  return <img src={src} alt={alt} className={className} onError={() => setFailedId(id)} />;
}
