package com.ruben.backend.trip.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("B8")
class AdminTripDTOUnitTest {

  @Test
  void fromCopiesTheTripAndItsCreatorsIdAndEmail() {
    User ana = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
    Trip trip =
        new Trip(ana, "Rome", "Italy", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17));
    ReflectionTestUtils.setField(trip, "id", 3L);

    AdminTripDTO dto = AdminTripDTO.from(trip);

    assertThat(dto)
        .isEqualTo(
            new AdminTripDTO(
                3L,
                "Rome",
                "Italy",
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 17),
                7L,
                "ana@example.com"));
  }
}
