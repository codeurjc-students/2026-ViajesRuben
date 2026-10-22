import { afterEach, expect, test, vi } from 'vitest';
import { ApiError } from './apiError';
import { imageUrl, uploadImage } from './imageService';

afterEach(() => {
  vi.restoreAllMocks();
});

test('uploads the file as multipart form data in the "file" part', async () => {
  const stored = { id: 'abc-123', contentType: 'image/png', size: 3 };
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({
    ok: true,
    json: () => Promise.resolve(stored),
  } as Response);
  const file = new File(['abc'], 'a.png', { type: 'image/png' });

  const result = await uploadImage(file);

  const [url, init] = vi.mocked(fetch).mock.calls[0];
  expect(String(url)).toContain('/api/v1/images');
  expect(init?.method).toBe('POST');
  expect(init?.body).toBeInstanceOf(FormData);
  expect((init?.body as FormData).get('file')).toBe(file);
  expect(result).toEqual(stored);
});

test('turns a rejected upload into an ApiError with the status', async () => {
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({ ok: false, status: 400 } as Response);

  await expect(uploadImage(new File(['x'], 'a.txt'))).rejects.toMatchObject({
    name: 'ApiError',
    status: 400,
  });
  await expect(uploadImage(new File(['x'], 'a.txt'))).rejects.toBeInstanceOf(ApiError);
});

test('builds the public URL of a stored image', () => {
  expect(imageUrl('abc-123')).toBe('/api/v1/images/abc-123');
});
