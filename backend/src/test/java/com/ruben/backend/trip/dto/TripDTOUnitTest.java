package com.ruben.backend.trip.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.image.entity.Image;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.UserStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("B4")
class TripDTOUnitTest {

  private static Trip trip() {
    Trip trip =
        new Trip(
            UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE),
            "Rome trip",
            "Rome",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 17));
    ReflectionTestUtils.setField(trip, "id", 3L);
    return trip;
  }

  @Test
  void mapsEveryFieldOfATripWithoutCover() {
    Trip trip = trip();
    trip.setBudget(new BigDecimal("300.00"));
    trip.setDescription("Ancient ruins");

    TripDTO dto = TripDTO.from(trip);

    assertThat(dto)
        .isEqualTo(
            new TripDTO(
                3L,
                7L,
                "Rome trip",
                "Rome",
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 17),
                new BigDecimal("300.00"),
                "Ancient ruins",
                null));
  }

  @Test
  void exposesTheCoverImageIdNotTheImage() {
    Trip trip = trip();
    Image cover = new Image(ImageTestData.png(10), "image/png");
    UUID id = UUID.randomUUID();
    ReflectionTestUtils.setField(cover, "id", id);
    trip.setCoverImage(cover);

    assertThat(TripDTO.from(trip).coverImageId()).isEqualTo(id);
  }
}
