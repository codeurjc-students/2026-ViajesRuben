import { expect, test } from 'vitest';
import { ApiError } from './apiError';
import { imageUrl, uploadImage } from './imageService';

const PNG_SIGNATURE = new Uint8Array([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);

test('the backend rejects an upload from someone who is not logged in', async () => {
  const file = new File([PNG_SIGNATURE], 'a.png', { type: 'image/png' });

  await expect(uploadImage(file)).rejects.toMatchObject({
    name: 'ApiError',
    status: 401,
  });
  await expect(uploadImage(file)).rejects.toBeInstanceOf(ApiError);
});

test('an image that does not exist is answered 404 at its public URL', async () => {
  const response = await fetch(imageUrl('00000000-0000-0000-0000-000000000000'));

  expect(response.status).toBe(404);
  expect((await response.json()).message).toBe('Image not found');
});
