package com.ruben.backend.invitation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruben.backend.invitation.entity.Invitation;
import com.ruben.backend.invitation.entity.InvitationStatus;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("I1")
class InvitationDTOUnitTest {

  @Test
  void copiesTheFieldsOfTheInvitation() {
    User ana = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
    Trip trip = new Trip(ana, "Trip", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17));
    Invitation invitation = new Invitation(trip, ana, "new@example.com", null, "secret-token");
    ReflectionTestUtils.setField(invitation, "id", 5L);

    InvitationDTO dto = InvitationDTO.from(invitation);

    assertThat(dto.id()).isEqualTo(5L);
    assertThat(dto.invitedEmail()).isEqualTo("new@example.com");
    assertThat(dto.status()).isEqualTo(InvitationStatus.PENDING);
    assertThat(dto.sentAt()).isEqualTo(invitation.getSentAt());
    assertThat(dto.respondedAt()).isNull();
  }

  @Test
  @Tag("I2")
  void showsWhenTheInvitationWasAnswered() {
    User ana = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
    Trip trip = new Trip(ana, "Trip", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17));
    Invitation invitation = new Invitation(trip, ana, "new@example.com", null, "secret-token");
    invitation.reject();

    InvitationDTO dto = InvitationDTO.from(invitation);

    assertThat(dto.status()).isEqualTo(InvitationStatus.REJECTED);
    assertThat(dto.respondedAt()).isEqualTo(invitation.getRespondedAt()).isNotNull();
  }

  @Test
  void neverCarriesTheToken() {
    assertThat(InvitationDTO.class.getRecordComponents())
        .extracting(RecordComponent::getName)
        .doesNotContain("token")
        .containsExactly("id", "invitedEmail", "status", "sentAt", "respondedAt");
  }
}
