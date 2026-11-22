/** Where an invitation stands: waiting, or answered by the invited person (I2). */
export type InvitationStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';

/** What the invited person answers. */
export type InvitationAnswer = Exclude<InvitationStatus, 'PENDING'>;

/** Mirrors the backend InvitationDTO, as the trip's creator sees it. The token of the link is never sent by the API. */
export interface Invitation {
  id: number;
  /** Lower case: the server normalises it. */
  invitedEmail: string;
  status: InvitationStatus;
  /** ISO instant (`2026-11-18T10:15:30Z`). */
  sentAt: string;
  /** When the invited person answered; null while pending. */
  respondedAt: string | null;
}

/** Mirrors the backend ReceivedInvitationDTO: an invitation as its addressee sees it, with no email or token. */
export interface ReceivedInvitation {
  id: number;
  tripId: number;
  tripName: string;
  inviterName: string;
  status: InvitationStatus;
  sentAt: string;
  respondedAt: string | null;
}
