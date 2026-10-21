import type { Trip } from '../types';

const SAMPLE_PARTICIPANTS = [6, 9, 2, 4];

const SAMPLE_IMAGES: Record<string, string> = {
  lisbon: '/Lisboa_example_image.png',
  rome: '/Roma_example_image.png',
  tokyo: '/Tokyo_example_image.png',
};

/**
 * Provisional values for what the backend does not return yet. Remove once TripDTO has
 * coverImage, participantCount and isCreator.
 */
export function tripDetails(trip: Trip): {
  participantCount: number;
  isCreator: boolean;
  coverImage?: string;
} {
  return {
    participantCount:
      trip.participantCount ?? SAMPLE_PARTICIPANTS[trip.id % SAMPLE_PARTICIPANTS.length],
    isCreator: trip.isCreator ?? trip.id % 2 === 1,
    coverImage: trip.coverImage ?? SAMPLE_IMAGES[trip.destination.toLowerCase()],
  };
}
