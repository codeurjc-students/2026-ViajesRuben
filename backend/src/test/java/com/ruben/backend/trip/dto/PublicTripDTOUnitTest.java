package com.ruben.backend.trip.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruben.backend.image.entity.Image;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("I7")
class PublicTripDTOUnitTest {

  private static Trip trip() {
    User author = new User("Ana García", "ana@tripmate.com", "hash", Role.USER);
    ReflectionTestUtils.setField(author, "id", 2L);
    Trip trip =
        new Trip(
            author, "Rome", "Rome, Italy", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 14));
    ReflectionTestUtils.setField(trip, "id", 7L);
    trip.setBudget(new BigDecimal("1200.00"));
    trip.setDescription("Five days");
    trip.publish(Instant.parse("2027-01-01T10:00:00Z"));
    return trip;
  }

  @Test
  void fromCopiesTheTripAndTheAuthorsName() {
    PublicTripDTO dto = PublicTripDTO.from(trip());

    assertThat(dto.id()).isEqualTo(7L);
    assertThat(dto.name()).isEqualTo("Rome");
    assertThat(dto.destination()).isEqualTo("Rome, Italy");
    assertThat(dto.startDate()).isEqualTo(LocalDate.of(2027, 7, 10));
    assertThat(dto.endDate()).isEqualTo(LocalDate.of(2027, 7, 14));
    assertThat(dto.budget()).isEqualByComparingTo("1200.00");
    assertThat(dto.description()).isEqualTo("Five days");
    assertThat(dto.authorName()).isEqualTo("Ana García");
    assertThat(dto.publishedAt()).isEqualTo(Instant.parse("2027-01-01T10:00:00Z"));
    assertThat(dto.coverImageId()).isNull();
  }

  @Test
  void fromReportsTheIdOfTheCover() {
    Trip trip = trip();
    Image cover = new Image(new byte[] {1}, "image/png");
    UUID id = UUID.randomUUID();
    ReflectionTestUtils.setField(cover, "id", id);
    trip.setCoverImage(cover);

    assertThat(PublicTripDTO.from(trip).coverImageId()).isEqualTo(id);
  }

  @Test
  void theDtoNeverExposesAnEmailTheAuthorsIdOrParticipants() {
    List<String> components =
        Arrays.stream(PublicTripDTO.class.getRecordComponents())
            .map(RecordComponent::getName)
            .toList();

    assertThat(components)
        .doesNotContain(
            "email", "authorEmail", "creatorEmail", "creatorId", "creator", "participants");
    assertThat(PublicTripDTO.from(trip()).toString()).doesNotContain("ana@tripmate.com");
  }
}
