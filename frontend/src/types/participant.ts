/** Mirrors the backend ParticipantDTO: someone who joined a trip (I3). The creator is not one of them. */
export interface Participant {
  userId: number;
  name: string;
  /** UUID of the picture, served by `GET /api/v1/images/{avatarId}`; null without avatar. */
  avatarId: string | null;
  /** Only the trip's creator receives it; null for everyone else. */
  email: string | null;
  /** ISO instant (`2026-11-18T10:15:30Z`). */
  joinedAt: string;
}
