import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { MAX_IMAGE_BYTES } from '../constants/images';
import { ApiError, NetworkError } from '../services/apiError';
import { uploadImage } from '../services/imageService';
import ImageUpload from './ImageUpload';

vi.mock('../services/imageService', () => ({
  uploadImage: vi.fn(),
  imageUrl: (id: string) => `/api/v1/images/${id}`,
}));

const stored = { id: 'abc-123', contentType: 'image/png', size: 4 };

beforeEach(() => {
  vi.mocked(uploadImage).mockReset();
});

function choose(file: File) {
  fireEvent.change(screen.getByLabelText('Portada'), { target: { files: [file] } });
}

function png(size = 4) {
  return new File([new Uint8Array(size)], 'a.png', { type: 'image/png' });
}

test('has a labelled file input that only offers JPEG, PNG and WebP', () => {
  render(<ImageUpload label="Portada" onUploaded={vi.fn()} />);

  expect(screen.getByLabelText('Portada')).toHaveAttribute(
    'accept',
    'image/jpeg,image/png,image/webp',
  );
});

test('uploads the chosen image, reports it and shows the stored result', async () => {
  vi.mocked(uploadImage).mockResolvedValue(stored);
  const onUploaded = vi.fn();
  render(<ImageUpload label="Portada" onUploaded={onUploaded} />);
  const file = png();

  choose(file);

  await waitFor(() => expect(onUploaded).toHaveBeenCalledWith(stored));
  expect(uploadImage).toHaveBeenCalledWith(file);
  expect(await screen.findByAltText('Imagen subida')).toHaveAttribute(
    'src',
    '/api/v1/images/abc-123',
  );
  expect(screen.queryByRole('alert')).not.toBeInTheDocument();
});

test('does nothing when the file dialog is cancelled', () => {
  render(<ImageUpload label="Portada" onUploaded={vi.fn()} />);

  fireEvent.change(screen.getByLabelText('Portada'), { target: { files: [] } });

  expect(uploadImage).not.toHaveBeenCalled();
});

test('rejects a file of the wrong type without calling the server', () => {
  const onUploaded = vi.fn();
  render(<ImageUpload label="Portada" onUploaded={onUploaded} />);

  choose(new File(['hello'], 'a.txt', { type: 'text/plain' }));

  expect(screen.getByRole('alert')).toHaveTextContent('Elige una imagen JPEG, PNG o WebP');
  expect(uploadImage).not.toHaveBeenCalled();
  expect(onUploaded).not.toHaveBeenCalled();
});

test('rejects an image over the size limit without calling the server', () => {
  render(<ImageUpload label="Portada" onUploaded={vi.fn()} />);

  choose(png(MAX_IMAGE_BYTES + 1));

  expect(screen.getByRole('alert')).toHaveTextContent('La imagen supera los 5 MB');
  expect(uploadImage).not.toHaveBeenCalled();
});

test.each([
  [new ApiError(401), 'Inicia sesión para subir imágenes.'],
  [new ApiError(400), 'El archivo no es una imagen válida'],
  [new ApiError(413), 'La imagen supera los 5 MB'],
  [new NetworkError(), 'No hay conexión con el servidor'],
  [new ApiError(500), 'No se pudo subir la imagen'],
])('shows what went wrong when the server fails (%#)', async (failure, message) => {
  vi.mocked(uploadImage).mockRejectedValue(failure);
  const onUploaded = vi.fn();
  render(<ImageUpload label="Portada" onUploaded={onUploaded} />);

  choose(png());

  expect(await screen.findByRole('alert')).toHaveTextContent(message);
  expect(onUploaded).not.toHaveBeenCalled();
  expect(screen.getByLabelText('Portada')).not.toBeDisabled();
});

test('clears the previous error when a new file is chosen', async () => {
  vi.mocked(uploadImage).mockResolvedValue(stored);
  render(<ImageUpload label="Portada" onUploaded={vi.fn()} />);
  choose(new File(['hello'], 'a.txt', { type: 'text/plain' }));
  expect(screen.getByRole('alert')).toBeInTheDocument();

  choose(png());

  await screen.findByAltText('Imagen subida');
  expect(screen.queryByRole('alert')).not.toBeInTheDocument();
});

test('disables the input while the image is uploading', async () => {
  let finish: (value: typeof stored) => void = () => {};
  vi.mocked(uploadImage).mockReturnValue(
    new Promise((resolve) => {
      finish = resolve;
    }),
  );
  render(<ImageUpload label="Portada" onUploaded={vi.fn()} />);

  choose(png());

  expect(screen.getByLabelText('Portada')).toBeDisabled();
  finish(stored);
  await waitFor(() => expect(screen.getByLabelText('Portada')).not.toBeDisabled());
});

test('B3 uses the given upload function instead of the default one', async () => {
  const upload = vi.fn().mockResolvedValue(stored);
  const onUploaded = vi.fn();
  render(<ImageUpload label="Portada" onUploaded={onUploaded} upload={upload} />);
  const file = png();

  choose(file);

  await waitFor(() => expect(onUploaded).toHaveBeenCalledWith(stored));
  expect(upload).toHaveBeenCalledWith(file);
  expect(uploadImage).not.toHaveBeenCalled();
});

test('B3 does not show the stored image when the preview is off', async () => {
  vi.mocked(uploadImage).mockResolvedValue(stored);
  const onUploaded = vi.fn();
  render(<ImageUpload label="Portada" onUploaded={onUploaded} preview={false} />);

  choose(png());

  await waitFor(() => expect(onUploaded).toHaveBeenCalled());
  expect(screen.queryByAltText('Imagen subida')).not.toBeInTheDocument();
});

describe('B4 choosing without uploading', () => {
  beforeEach(() => {
    URL.createObjectURL = vi.fn(() => 'blob:preview');
    URL.revokeObjectURL = vi.fn();
  });

  test('hands the valid file over, previews it and does not upload', () => {
    const onSelect = vi.fn();
    const onUploaded = vi.fn();
    render(<ImageUpload label="Portada" onUploaded={onUploaded} onSelect={onSelect} />);
    const file = png();

    choose(file);

    expect(onSelect).toHaveBeenCalledWith(file);
    expect(screen.getByAltText('Vista previa')).toHaveAttribute('src', 'blob:preview');
    expect(uploadImage).not.toHaveBeenCalled();
    expect(onUploaded).not.toHaveBeenCalled();
    expect(screen.getByLabelText('Portada')).not.toBeDisabled();
  });

  test('still rejects a wrong type or size without selecting it', () => {
    const onSelect = vi.fn();
    render(<ImageUpload label="Portada" onUploaded={vi.fn()} onSelect={onSelect} />);

    choose(new File(['hello'], 'a.txt', { type: 'text/plain' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Elige una imagen JPEG, PNG o WebP');
    choose(png(MAX_IMAGE_BYTES + 1));
    expect(screen.getByRole('alert')).toHaveTextContent('La imagen supera los 5 MB');

    expect(onSelect).not.toHaveBeenCalled();
    expect(screen.queryByAltText('Vista previa')).not.toBeInTheDocument();
  });

  test('releases the preview URL when it is replaced and when the component goes away', () => {
    const urls = ['blob:one', 'blob:two'];
    URL.createObjectURL = vi.fn(() => urls.shift() as string);
    const { unmount } = render(
      <ImageUpload label="Portada" onUploaded={vi.fn()} onSelect={vi.fn()} />,
    );

    choose(png());
    choose(png());
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:one');
    unmount();

    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:two');
  });

  test('does not show the preview when it is turned off', () => {
    render(<ImageUpload label="Portada" onUploaded={vi.fn()} onSelect={vi.fn()} preview={false} />);

    choose(png());

    expect(screen.queryByAltText('Vista previa')).not.toBeInTheDocument();
  });
});

test('B3 explains a 403 from the server', async () => {
  vi.mocked(uploadImage).mockRejectedValue(new ApiError(403));
  render(<ImageUpload label="Portada" onUploaded={vi.fn()} />);

  choose(png());

  expect(await screen.findByRole('alert')).toHaveTextContent('No puedes cambiar esta imagen.');
});
