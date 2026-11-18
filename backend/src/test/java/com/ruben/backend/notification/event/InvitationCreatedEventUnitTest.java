package com.ruben.backend.notification.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("I1")
class InvitationCreatedEventUnitTest {

  @Test
  void itsTextShowsTheInvitationIdButNeitherTheTokenNorTheEmail() {
    InvitationCreatedEvent event =
        new InvitationCreatedEvent(5L, "friend@example.com", "secret-token", "Ana", "Rome");

    assertThat(event).hasToString("InvitationCreatedEvent[invitationId=5]");
    assertThat(event.toString()).doesNotContain("secret-token", "friend@example.com");
  }

  @Test
  void itCarriesWhatTheEmailNeeds() {
    InvitationCreatedEvent event =
        new InvitationCreatedEvent(5L, "friend@example.com", "secret-token", "Ana", "Rome");

    assertThat(event.invitationId()).isEqualTo(5L);
    assertThat(event.invitedEmail()).isEqualTo("friend@example.com");
    assertThat(event.token()).isEqualTo("secret-token");
    assertThat(event.inviterName()).isEqualTo("Ana");
    assertThat(event.tripName()).isEqualTo("Rome");
  }
}
