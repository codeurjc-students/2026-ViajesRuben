package com.ruben.backend.participation.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Someone who joined a trip. Everyone who can see the trip sees the name, the avatar and the join
 * date; the email is only for the trip's creator and is null for anyone else. The avatar bytes are
 * served by `GET /api/v1/images/{avatarId}`, and `avatarId` is null when the user has no avatar.
 */
public record ParticipantDTO(
    Long userId, String name, UUID avatarId, String email, Instant joinedAt) {

  /** The same participant without the email, for everyone but the trip's creator. */
  public ParticipantDTO withoutEmail() {
    return new ParticipantDTO(userId, name, avatarId, null, joinedAt);
  }
}
