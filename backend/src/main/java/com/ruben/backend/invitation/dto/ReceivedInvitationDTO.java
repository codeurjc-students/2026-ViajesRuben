package com.ruben.backend.invitation.dto;

import com.ruben.backend.invitation.entity.Invitation;
import com.ruben.backend.invitation.entity.InvitationStatus;
import java.time.Instant;

/**
 * An invitation as its addressee sees it: the trip and who invites, never the email, the token or
 * any user id.
 */
public record ReceivedInvitationDTO(
    Long id,
    Long tripId,
    String tripName,
    String inviterName,
    InvitationStatus status,
    Instant sentAt,
    Instant respondedAt) {

  public static ReceivedInvitationDTO from(Invitation invitation) {
    return new ReceivedInvitationDTO(
        invitation.getId(),
        invitation.getTrip().getId(),
        invitation.getTrip().getName(),
        invitation.getInviter().getName(),
        invitation.getStatus(),
        invitation.getSentAt(),
        invitation.getRespondedAt());
  }
}
