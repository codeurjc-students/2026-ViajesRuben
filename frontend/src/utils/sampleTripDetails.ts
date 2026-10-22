import type { Trip } from '../types';

const SAMPLE_PARTICIPANTS = [6, 9, 2, 4];

/**
 * Provisional values for what the backend does not return yet. Remove once TripDTO has
 * participantCount and isCreator.
 */
export function tripDetails(trip: Trip): {
  participantCount: number;
  isCreator: boolean;
} {
  return {
    participantCount:
      trip.participantCount ?? SAMPLE_PARTICIPANTS[trip.id % SAMPLE_PARTICIPANTS.length],
    isCreator: trip.isCreator ?? trip.id % 2 === 1,
  };
}
