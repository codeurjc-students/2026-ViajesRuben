package com.ruben.backend.invitation.dto;

import com.ruben.backend.invitation.entity.Invitation;
import com.ruben.backend.invitation.entity.InvitationStatus;
import java.time.Instant;

/**
 * What the API says about an invitation to its creator. The token is never part of it: only the
 * email has it. `respondedAt` is null while the invitation is pending.
 */
public record InvitationDTO(
    Long id, String invitedEmail, InvitationStatus status, Instant sentAt, Instant respondedAt) {

  public static InvitationDTO from(Invitation invitation) {
    return new InvitationDTO(
        invitation.getId(),
        invitation.getInvitedEmail(),
        invitation.getStatus(),
        invitation.getSentAt(),
        invitation.getRespondedAt());
  }
}
