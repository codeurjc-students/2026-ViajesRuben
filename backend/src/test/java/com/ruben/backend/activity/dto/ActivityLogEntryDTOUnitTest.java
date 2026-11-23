package com.ruben.backend.activity.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruben.backend.activity.entity.Activity;
import com.ruben.backend.activity.entity.ActivityLogAction;
import com.ruben.backend.activity.entity.ActivityLogEntry;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("I4")
class ActivityLogEntryDTOUnitTest {

  private User bob;
  private Trip trip;
  private Activity activity;

  @BeforeEach
  void setUp() {
    User ana = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
    bob = UserTestData.user(8L, "bob@example.com", Role.USER, UserStatus.ACTIVE);
    trip = new Trip(ana, "Trip", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17));
    activity = new Activity(trip, ana, "Colosseum", LocalDate.of(2027, 7, 11));
    ReflectionTestUtils.setField(activity, "id", 5L);
  }

  private ActivityLogEntry entry(Activity linked, ActivityLogAction action, String changedFields) {
    ActivityLogEntry entry =
        new ActivityLogEntry(trip, linked, "Colosseum", bob, "Bob", action, changedFields);
    ReflectionTestUtils.setField(entry, "id", 3L);
    return entry;
  }

  @Test
  void mapsTheIdsTheSnapshotNamesTheActionAndTheTime() {
    ActivityLogEntry entry = entry(activity, ActivityLogAction.CREATED, null);

    ActivityLogEntryDTO dto = ActivityLogEntryDTO.from(entry);

    assertThat(dto.id()).isEqualTo(3L);
    assertThat(dto.activityId()).isEqualTo(5L);
    assertThat(dto.activityName()).isEqualTo("Colosseum");
    assertThat(dto.userId()).isEqualTo(8L);
    assertThat(dto.userName()).isEqualTo("Bob");
    assertThat(dto.action()).isEqualTo(ActivityLogAction.CREATED);
    assertThat(dto.createdAt()).isEqualTo(entry.getCreatedAt());
  }

  @Test
  void anEditListsTheChangedFieldsAsSeparateNames() {
    ActivityLogEntryDTO dto =
        ActivityLogEntryDTO.from(
            entry(activity, ActivityLogAction.UPDATED, "name,startTime,image"));

    assertThat(dto.changedFields()).containsExactly("name", "startTime", "image");
  }

  @Test
  void anyOtherLineHasAnEmptyListOfChangedFieldsNeverNull() {
    assertThat(
            ActivityLogEntryDTO.from(entry(activity, ActivityLogAction.CREATED, null))
                .changedFields())
        .isEmpty();
    assertThat(
            ActivityLogEntryDTO.from(entry(activity, ActivityLogAction.UPDATED, null))
                .changedFields())
        .isEmpty();
  }

  @Test
  void aDeletedActivityHasNoIdButKeepsItsName() {
    ActivityLogEntryDTO dto =
        ActivityLogEntryDTO.from(entry(null, ActivityLogAction.DELETED, null));

    assertThat(dto.activityId()).isNull();
    assertThat(dto.activityName()).isEqualTo("Colosseum");
    assertThat(dto.action()).isEqualTo(ActivityLogAction.DELETED);
  }
}
