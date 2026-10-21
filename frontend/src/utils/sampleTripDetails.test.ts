import { test, expect } from 'vitest';
import { tripDetails } from './sampleTripDetails';

const base = { id: 1, name: 'T', destination: 'D', startDate: '2027-05-01', endDate: '2027-05-02' };

test('uses the values sent by the API when present', () => {
  expect(tripDetails({ ...base, participantCount: 3, isCreator: false })).toEqual({
    participantCount: 3,
    isCreator: false,
  });
});

test('falls back to sample values when the API does not send them', () => {
  const details = tripDetails(base);

  expect(details.participantCount).toBeGreaterThan(0);
  expect(typeof details.isCreator).toBe('boolean');
});

test('picks the sample cover image of known destinations', () => {
  expect(tripDetails({ ...base, destination: 'Lisbon' }).coverImage).toBe(
    '/Lisboa_example_image.png',
  );
  expect(tripDetails({ ...base, destination: 'Rome' }).coverImage).toBe('/Roma_example_image.png');
  expect(tripDetails({ ...base, destination: 'Tokyo' }).coverImage).toBe(
    '/Tokyo_example_image.png',
  );
});

test('has no cover image for unknown destinations and keeps the one sent by the API', () => {
  expect(tripDetails({ ...base, destination: 'Oslo' }).coverImage).toBeUndefined();
  expect(tripDetails({ ...base, destination: 'Rome', coverImage: '/own.png' }).coverImage).toBe(
    '/own.png',
  );
});
