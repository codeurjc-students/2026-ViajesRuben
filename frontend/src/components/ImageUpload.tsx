import { useId, useState, type ChangeEvent } from 'react';
import Alert from 'react-bootstrap/Alert';
import Form from 'react-bootstrap/Form';
import { ALLOWED_IMAGE_TYPES, MAX_IMAGE_BYTES } from '../constants/images';
import { ApiError, NetworkError } from '../services/apiError';
import { uploadImage } from '../services/imageService';
import type { ImageInfo } from '../types';
import StoredImage from './StoredImage';

const WRONG_TYPE = 'El archivo no es una imagen válida. Elige una imagen JPEG, PNG o WebP.';
const MAX_MB = MAX_IMAGE_BYTES / 1024 / 1024;
const TOO_BIG = `La imagen supera los ${MAX_MB} MB. Elige una más pequeña.`;

function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 401) return 'Inicia sesión para subir imágenes.';
    if (error.status === 400) return WRONG_TYPE;
    if (error.status === 413) return TOO_BIG;
  }
  if (error instanceof NetworkError) return 'No hay conexión con el servidor. Inténtalo de nuevo.';
  return 'No se pudo subir la imagen. Inténtalo de nuevo.';
}

interface ImageUploadProps {
  label?: string;
  /** Called with the stored image once the server has accepted it. */
  onUploaded: (image: ImageInfo) => void;
}

/** File picker that uploads the chosen image and shows the stored result. */
export default function ImageUpload({ label = 'Imagen', onUploaded }: ImageUploadProps) {
  const inputId = useId();
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [uploaded, setUploaded] = useState<ImageInfo | null>(null);

  async function handleChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;
    setError(null);
    if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
      setError(WRONG_TYPE);
      return;
    }
    if (file.size > MAX_IMAGE_BYTES) {
      setError(TOO_BIG);
      return;
    }
    setUploading(true);
    try {
      const image = await uploadImage(file);
      setUploaded(image);
      onUploaded(image);
    } catch (failure) {
      setError(errorMessage(failure));
    } finally {
      setUploading(false);
    }
  }

  return (
    <Form.Group controlId={inputId}>
      <Form.Label>{label}</Form.Label>
      <Form.Control
        type="file"
        accept={ALLOWED_IMAGE_TYPES.join(',')}
        disabled={uploading}
        onChange={handleChange}
      />
      <Form.Text>{`JPEG, PNG o WebP, de hasta ${MAX_MB} MB.`}</Form.Text>
      {error && (
        <Alert variant="danger" className="mt-2 mb-0">
          {error}
        </Alert>
      )}
      {uploaded && (
        <StoredImage id={uploaded.id} alt="Imagen subida" className="mt-2 img-fluid rounded" />
      )}
    </Form.Group>
  );
}
