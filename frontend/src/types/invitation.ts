/** Where an invitation stands; the accepted and rejected states arrive with I2. */
export type InvitationStatus = 'PENDING';

/** Mirrors the backend InvitationDTO. The token of the link is never sent by the API. */
export interface Invitation {
  id: number;
  /** Lower case: the server normalises it. */
  invitedEmail: string;
  status: InvitationStatus;
  /** ISO instant (`2026-11-18T10:15:30Z`). */
  sentAt: string;
}
