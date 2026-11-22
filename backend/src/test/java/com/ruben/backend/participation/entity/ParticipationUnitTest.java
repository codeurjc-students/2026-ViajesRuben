package com.ruben.backend.participation.entity;

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

@Tag("I2")
class ParticipationUnitTest {

  @Test
  void keepsTheTripAndTheUserAndJoinsNowWithMicrosecondPrecision() {
    User ana = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
    User carlos = UserTestData.user(8L, "carlos@example.com", Role.USER, UserStatus.ACTIVE);
    Trip trip = new Trip(ana, "Trip", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17));
    Instant before = Instant.now();

    Participation participation = new Participation(trip, carlos);

    assertThat(participation.getTrip()).isSameAs(trip);
    assertThat(participation.getUser()).isSameAs(carlos);
    assertThat(participation.getId()).isNull();
    assertThat(participation.getJoinedAt()).isBetween(before.minusNanos(1000), Instant.now());
    assertThat(participation.getJoinedAt().getNano() % 1000).isZero();
  }
}
