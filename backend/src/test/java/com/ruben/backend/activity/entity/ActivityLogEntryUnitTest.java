package com.ruben.backend.activity.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("I4")
class ActivityLogEntryUnitTest {

  private User ana;
  private User bob;
  private Trip trip;
  private Activity activity;

  @BeforeEach
  void setUp() {
    ana = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
    bob = UserTestData.user(8L, "bob@example.com", Role.USER, UserStatus.ACTIVE);
    trip = new Trip(ana, "Trip", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17));
    activity = new Activity(trip, ana, "Colosseum", LocalDate.of(2027, 7, 11));
  }

  @Test
  void keepsWhatItWasGivenAndStampsTheTimeOfTheChange() {
    Instant before = Instant.now().minusSeconds(1);

    ActivityLogEntry entry =
        new ActivityLogEntry(
            trip, activity, "Colosseum", bob, "Bob", ActivityLogAction.UPDATED, "name,date");

    assertThat(entry.getId()).isNull();
    assertThat(entry.getTrip()).isSameAs(trip);
    assertThat(entry.getActivity()).isSameAs(activity);
    assertThat(entry.getActivityName()).isEqualTo("Colosseum");
    assertThat(entry.getUser()).isSameAs(bob);
    assertThat(entry.getUserName()).isEqualTo("Bob");
    assertThat(entry.getAction()).isEqualTo(ActivityLogAction.UPDATED);
    assertThat(entry.getChangedFields()).isEqualTo("name,date");
    assertThat(entry.getCreatedAt()).isBetween(before, Instant.now().plusSeconds(1));
  }

  @Test
  void theTimeIsCutToMicrosecondsLikeTheDatabaseKeepsIt() {
    ActivityLogEntry entry =
        new ActivityLogEntry(
            trip, activity, "Colosseum", bob, "Bob", ActivityLogAction.CREATED, null);

    assertThat(entry.getCreatedAt()).isEqualTo(entry.getCreatedAt().truncatedTo(ChronoUnit.MICROS));
  }

  @Test
  void aDeletionHasNoActivityAndAnythingButAnEditHasNoChangedFields() {
    ActivityLogEntry entry =
        new ActivityLogEntry(trip, null, "Colosseum", bob, "Bob", ActivityLogAction.DELETED, null);

    assertThat(entry.getActivity()).isNull();
    assertThat(entry.getActivityName()).isEqualTo("Colosseum");
    assertThat(entry.getChangedFields()).isNull();
    assertThat(entry.getAction()).isEqualTo(ActivityLogAction.DELETED);
  }

  @Test
  void theActionsAreTheThreeKindsOfChange() {
    assertThat(ActivityLogAction.values())
        .containsExactly(
            ActivityLogAction.CREATED, ActivityLogAction.UPDATED, ActivityLogAction.DELETED);
    assertThat(ActivityLogAction.valueOf("UPDATED")).isEqualTo(ActivityLogAction.UPDATED);
  }
}
