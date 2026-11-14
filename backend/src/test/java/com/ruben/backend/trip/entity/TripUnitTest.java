package com.ruben.backend.trip.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.UserStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("I7")
class TripUnitTest {

  private static Trip trip() {
    return new Trip(
        UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE),
        "Rome",
        "Rome",
        LocalDate.of(2027, 7, 10),
        LocalDate.of(2027, 7, 17));
  }

  @Test
  void aNewTripIsNotPublished() {
    Trip trip = trip();

    assertThat(trip.isPublished()).isFalse();
    assertThat(trip.getPublishedAt()).isNull();
  }

  @Test
  void publishingSetsTheFlagAndTheDate() {
    Trip trip = trip();
    // The date is kept in microseconds, so the lower bound is cut the same way.
    Instant before = Instant.now().truncatedTo(ChronoUnit.MICROS);

    trip.publish();

    assertThat(trip.isPublished()).isTrue();
    assertThat(trip.getPublishedAt()).isBetween(before, Instant.now());
  }

  @Test
  void thePublicationDateIsCutToMicrosecondsLikeTheDatabaseKeepsIt() {
    Trip trip = trip();

    trip.publish(Instant.parse("2027-01-01T10:00:00.123456789Z"));

    assertThat(trip.getPublishedAt()).isEqualTo(Instant.parse("2027-01-01T10:00:00.123456Z"));
  }

  @Test
  void publishingAPublishedTripKeepsTheFirstDate() {
    Trip trip = trip();
    Instant first = Instant.parse("2027-01-01T10:00:00Z");
    trip.publish(first);

    trip.publish();
    trip.publish(Instant.parse("2028-01-01T10:00:00Z"));

    assertThat(trip.getPublishedAt()).isEqualTo(first);
  }

  @Test
  void unpublishingClearsTheFlagAndTheDateAndPublishingAgainGetsANewDate() {
    Trip trip = trip();
    trip.publish(Instant.parse("2027-01-01T10:00:00Z"));

    trip.unpublish();

    assertThat(trip.isPublished()).isFalse();
    assertThat(trip.getPublishedAt()).isNull();
    trip.publish(Instant.parse("2027-02-02T10:00:00Z"));
    assertThat(trip.getPublishedAt()).isEqualTo(Instant.parse("2027-02-02T10:00:00Z"));
  }

  @Test
  void unpublishingATripThatIsNotPublishedChangesNothing() {
    Trip trip = trip();

    trip.unpublish();

    assertThat(trip.isPublished()).isFalse();
    assertThat(trip.getPublishedAt()).isNull();
  }
}
