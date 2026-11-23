package com.ruben.backend.participation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("I3")
class ParticipantDTOUnitTest {

  @Test
  void withoutEmailKeepsEverythingElseAndTheAvatarId() {
    UUID avatar = UUID.randomUUID();
    Instant joinedAt = Instant.parse("2027-01-01T10:00:00Z");
    ParticipantDTO participant = new ParticipantDTO(8L, "Bob", avatar, "bob@example.com", joinedAt);

    ParticipantDTO hidden = participant.withoutEmail();

    assertThat(hidden).isEqualTo(new ParticipantDTO(8L, "Bob", avatar, null, joinedAt));
    assertThat(participant.email()).isEqualTo("bob@example.com");
  }
}
