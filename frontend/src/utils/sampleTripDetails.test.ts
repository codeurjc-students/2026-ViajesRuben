import { test, expect } from 'vitest';
import { tripOf } from '../testHelpers';
import { tripDetails } from './sampleTripDetails';

const base = tripOf({ id: 1, name: 'T', destination: 'D' });

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
