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

@Tag("I2")
class ReceivedInvitationDTOUnitTest {

  @Test
  void showsTheTripAndTheInviterOfTheInvitation() {
    User ana = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
    Trip trip = new Trip(ana, "Trip", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17));
    ReflectionTestUtils.setField(trip, "id", 1L);
    Invitation invitation = new Invitation(trip, ana, "carlos@example.com", null, "secret-token");
    ReflectionTestUtils.setField(invitation, "id", 5L);

    ReceivedInvitationDTO dto = ReceivedInvitationDTO.from(invitation);

    assertThat(dto.id()).isEqualTo(5L);
    assertThat(dto.tripId()).isEqualTo(1L);
    assertThat(dto.tripName()).isEqualTo("Trip");
    assertThat(dto.inviterName()).isEqualTo(ana.getName());
    assertThat(dto.status()).isEqualTo(InvitationStatus.PENDING);
    assertThat(dto.sentAt()).isEqualTo(invitation.getSentAt());
    assertThat(dto.respondedAt()).isNull();
  }

  @Test
  void neverCarriesTheTokenTheEmailOrAUserId() {
    assertThat(ReceivedInvitationDTO.class.getRecordComponents())
        .extracting(RecordComponent::getName)
        .containsExactly(
            "id", "tripId", "tripName", "inviterName", "status", "sentAt", "respondedAt");
  }
}
