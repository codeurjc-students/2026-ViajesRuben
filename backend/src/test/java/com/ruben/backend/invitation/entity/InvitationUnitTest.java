package com.ruben.backend.invitation.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("I1")
class InvitationUnitTest {

  private final User ana = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
  private final Trip trip =
      new Trip(ana, "Trip", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17));

  @Test
  void aNewInvitationIsPendingAndKeepsWhatItWasGiven() {
    User carlos = UserTestData.user(8L, "carlos@example.com", Role.USER, UserStatus.ACTIVE);

    Invitation invitation = new Invitation(trip, ana, "carlos@example.com", carlos, "token-1");

    assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
    assertThat(invitation.getTrip()).isSameAs(trip);
    assertThat(invitation.getInviter()).isSameAs(ana);
    assertThat(invitation.getInvitedEmail()).isEqualTo("carlos@example.com");
    assertThat(invitation.getInvitedUser()).isSameAs(carlos);
    assertThat(invitation.getToken()).isEqualTo("token-1");
    assertThat(invitation.getId()).isNull();
  }

  @Test
  @Tag("I2")
  void acceptingRecordsTheAnswerAndTheAccountWhenThereWasNone() {
    User carlos = UserTestData.user(8L, "carlos@example.com", Role.USER, UserStatus.ACTIVE);
    Invitation invitation = new Invitation(trip, ana, "carlos@example.com", null, "token-1");
    assertThat(invitation.getRespondedAt()).isNull();

    invitation.accept(carlos);

    assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
    assertThat(invitation.getInvitedUser()).isSameAs(carlos);
    assertThat(invitation.getRespondedAt()).isNotNull();
    assertThat(invitation.getRespondedAt().getNano() % 1000).isZero();
  }

  @Test
  @Tag("I2")
  void acceptingKeepsTheAccountTheInvitationAlreadyHad() {
    User carlos = UserTestData.user(8L, "carlos@example.com", Role.USER, UserStatus.ACTIVE);
    User other = UserTestData.user(9L, "other@example.com", Role.USER, UserStatus.ACTIVE);
    Invitation invitation = new Invitation(trip, ana, "carlos@example.com", carlos, "token-1");

    invitation.accept(other);

    assertThat(invitation.getInvitedUser()).isSameAs(carlos);
  }

  @Test
  @Tag("I2")
  void rejectingRecordsTheAnswer() {
    Invitation invitation = new Invitation(trip, ana, "carlos@example.com", null, "token-1");

    invitation.reject();

    assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.REJECTED);
    assertThat(invitation.getInvitedUser()).isNull();
    assertThat(invitation.getRespondedAt()).isNotNull();
  }

  @Test
  void theSendingTimeIsNowAndHasNoMoreThanMicroseconds() {
    Instant before = Instant.now();

    Instant sentAt = new Invitation(trip, ana, "a@example.com", null, "token").getSentAt();

    assertThat(sentAt).isBetween(before.minusNanos(1000), Instant.now());
    assertThat(sentAt.getNano() % 1000).isZero();
  }
}
