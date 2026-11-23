package com.ruben.backend.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import com.ruben.backend.activity.entity.Activity;
import com.ruben.backend.activity.entity.ActivityLogAction;
import com.ruben.backend.activity.entity.ActivityLogEntry;
import com.ruben.backend.activity.repository.ActivityLogEntryRepository;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AuthService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("I4")
@ExtendWith(MockitoExtension.class)
class ActivityLogServiceUnitTest {

  @Mock private ActivityLogEntryRepository entryRepository;
  @Mock private UserRepository userRepository;
  @Mock private AuthService authService;

  private ActivityLogService activityLogService;
  private User ana;
  private User bob;
  private Trip trip;
  private Activity activity;

  @BeforeEach
  void setUp() {
    activityLogService = new ActivityLogService(entryRepository, userRepository, authService);
    ana = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
    bob = UserTestData.user(8L, "bob@example.com", Role.USER, UserStatus.ACTIVE);
    bob.setName("Bob");
    trip = new Trip(ana, "Trip", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17));
    ReflectionTestUtils.setField(trip, "id", 1L);
    activity = new Activity(trip, ana, "Colosseum", LocalDate.of(2027, 7, 11));
    ReflectionTestUtils.setField(activity, "id", 5L);
    lenient().when(authService.getCurrentUser()).thenReturn(UserDTO.from(bob));
    lenient().when(userRepository.getReferenceById(8L)).thenReturn(bob);
  }

  private ActivityLogEntry savedEntry() {
    ArgumentCaptor<ActivityLogEntry> saved = ArgumentCaptor.forClass(ActivityLogEntry.class);
    verify(entryRepository).save(saved.capture());
    return saved.getValue();
  }

  @Test
  void aCreatedLineKeepsTheTripTheActivityAndTheCurrentUserWithTheirNames() {
    activityLogService.recordCreated(trip, activity);

    ActivityLogEntry entry = savedEntry();
    assertThat(entry.getTrip()).isSameAs(trip);
    assertThat(entry.getActivity()).isSameAs(activity);
    assertThat(entry.getActivityName()).isEqualTo("Colosseum");
    assertThat(entry.getUser()).isSameAs(bob);
    assertThat(entry.getUserName()).isEqualTo("Bob");
    assertThat(entry.getAction()).isEqualTo(ActivityLogAction.CREATED);
    assertThat(entry.getChangedFields()).isNull();
    assertThat(entry.getCreatedAt()).isNotNull();
  }

  @Test
  void anUpdatedLineListsTheChangedFieldNamesSeparatedByCommas() {
    activityLogService.recordUpdated(trip, activity, List.of("name", "date", "cost"));

    ActivityLogEntry entry = savedEntry();
    assertThat(entry.getAction()).isEqualTo(ActivityLogAction.UPDATED);
    assertThat(entry.getActivity()).isSameAs(activity);
    assertThat(entry.getChangedFields()).isEqualTo("name,date,cost");
  }

  @Test
  void anUpdatedLineWithoutChangedFieldsStoresNoFieldList() {
    activityLogService.recordUpdated(trip, activity, List.of());

    assertThat(savedEntry().getChangedFields()).isNull();
  }

  @Test
  void aDeletedLineHasNoActivityAndKeepsTheNameItHad() {
    activityLogService.recordDeleted(trip, "Colosseum");

    ActivityLogEntry entry = savedEntry();
    assertThat(entry.getAction()).isEqualTo(ActivityLogAction.DELETED);
    assertThat(entry.getActivity()).isNull();
    assertThat(entry.getActivityName()).isEqualTo("Colosseum");
    assertThat(entry.getUserName()).isEqualTo("Bob");
    assertThat(entry.getChangedFields()).isNull();
  }

  @Test
  void deletingTheLogOfATripDeletesItsLinesInOneQuery() {
    activityLogService.deleteAllByTrip(1L);

    verify(entryRepository).deleteByTripId(1L);
  }

  @Test
  void detachingAnActivityMakesItsLinesForgetItWithoutLoadingThem() {
    activityLogService.detachActivity(5L);

    verify(entryRepository).detachActivity(5L);
  }
}
