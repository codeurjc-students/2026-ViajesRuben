package com.ruben.backend.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.activity.dto.ActivityDTO;
import com.ruben.backend.activity.entity.Activity;
import com.ruben.backend.activity.repository.ActivityRepository;
import com.ruben.backend.activity.service.ActivityService;
import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.trip.dto.DayPreviewDTO;
import com.ruben.backend.trip.dto.ExampleTripDTO;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** No user is logged in: the example trips are public, so none of these calls needs one. */
@Tag("B1")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ExampleTripServiceIntegrationTest {

  private static final LocalDate START = LocalDate.of(2027, 7, 10);

  @Autowired private ExampleTripService exampleTripService;
  @Autowired private ActivityService activityService;
  @Autowired private TripRepository tripRepository;
  @Autowired private ActivityRepository activityRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private DatabaseCleaner databaseCleaner;

  private User admin;
  private User ana;

  @BeforeEach
  void setUp() {
    databaseCleaner.clear();
    admin = userRepository.save(new User("Admin", "admin@example.com", "hash", Role.ADMIN));
    ana = userRepository.save(new User("Ana", "ana@example.com", "hash", Role.USER));
  }

  private Trip trip(User creator, String name, int startOffset) {
    LocalDate start = START.plusDays(startOffset);
    return tripRepository.save(new Trip(creator, name, "City", start, start.plusDays(6)));
  }

  private Activity activity(Trip trip, int day, String name, LocalTime start) {
    Activity activity =
        new Activity(trip, trip.getCreator(), name, trip.getStartDate().plusDays(day));
    activity.setStartTime(start);
    return activityRepository.save(activity);
  }

  private static PageRequest byStart(int page, int size) {
    return PageRequest.of(page, size, Sort.by("startDate", "id"));
  }

  @Test
  void findPageListsOnlyTheAdministratorsTripsAndPagesThem() {
    for (int i = 0; i < 4; i++) {
      trip(admin, "Example " + i, i);
    }
    trip(ana, "Ana's trip", 0);

    PageDTO<ExampleTripDTO> first = exampleTripService.findPage(byStart(0, 3));
    PageDTO<ExampleTripDTO> second = exampleTripService.findPage(byStart(1, 3));

    assertThat(first.totalElements()).isEqualTo(4);
    assertThat(first.content()).hasSize(3);
    assertThat(first.last()).isFalse();
    assertThat(second.content()).hasSize(1);
    assertThat(second.last()).isTrue();
    assertThat(second.content().get(0).trip().name()).isEqualTo("Example 3");
  }

  @Test
  void theFirstDaysShowTheFirstActivityOfEachOfTheFirstThreeDays() {
    Trip rome = trip(admin, "Rome", 0);
    activity(rome, 0, "Lunch", LocalTime.of(13, 0));
    activity(rome, 0, "Colosseum", LocalTime.of(9, 0));
    activity(rome, 2, "Vatican", LocalTime.of(10, 0));
    activity(rome, 3, "Fourth day", LocalTime.of(10, 0));
    // Day 2 (the second) has no activities and is left out.

    ExampleTripDTO example = exampleTripService.findPage(byStart(0, 3)).content().get(0);

    assertThat(example.firstDays())
        .containsExactly(new DayPreviewDTO(1, "Colosseum"), new DayPreviewDTO(3, "Vatican"));
  }

  @Test
  void anActivityWithoutStartTimeIsTheFirstOfItsDayAndTiesGoToTheEarliestCreated() {
    Trip rome = trip(admin, "Rome", 0);
    activity(rome, 0, "Breakfast", LocalTime.of(8, 0));
    activity(rome, 0, "Whole day", null);
    activity(rome, 1, "Second whole day", null);
    activity(rome, 1, "Another whole day", null);
    activity(rome, 2, "Early", LocalTime.of(9, 0));
    activity(rome, 2, "Also early", LocalTime.of(9, 0));

    ExampleTripDTO example = exampleTripService.findPage(byStart(0, 3)).content().get(0);

    assertThat(example.firstDays())
        .containsExactly(
            new DayPreviewDTO(1, "Whole day"),
            new DayPreviewDTO(2, "Second whole day"),
            new DayPreviewDTO(3, "Early"));
  }

  @Test
  void thePreviewsOfAPageDoNotMixTheTripsAndIgnoreActivitiesOfOtherUsers() {
    Trip rome = trip(admin, "Rome", 0);
    Trip paris = trip(admin, "Paris", 1);
    Trip anas = trip(ana, "Ana's trip", 0);
    activity(rome, 0, "Colosseum", LocalTime.of(9, 0));
    activity(paris, 1, "Louvre", LocalTime.of(9, 0));
    activity(anas, 0, "Private", LocalTime.of(9, 0));

    PageDTO<ExampleTripDTO> page = exampleTripService.findPage(byStart(0, 3));

    assertThat(page.content()).hasSize(2);
    assertThat(page.content().get(0).firstDays())
        .containsExactly(new DayPreviewDTO(1, "Colosseum"));
    assertThat(page.content().get(1).firstDays()).containsExactly(new DayPreviewDTO(2, "Louvre"));
  }

  @Test
  void findByIdAndTheActivitiesOfAnExampleTripAreReadableWithoutLogin() {
    Trip rome = trip(admin, "Rome", 0);
    activity(rome, 0, "Colosseum", LocalTime.of(9, 0));
    activity(rome, 1, "Vatican", LocalTime.of(9, 0));

    assertThat(exampleTripService.findById(rome.getId()).name()).isEqualTo("Rome");
    PageDTO<ActivityDTO> day =
        activityService.findExamplePage(
            rome.getId(), START.plusDays(1), PageRequest.of(0, 10, Sort.by("date", "id")));
    assertThat(day.content()).extracting(ActivityDTO::name).containsExactly("Vatican");
  }

  @Test
  void aTripOfARegularUserIsNotFoundAsAnExample() {
    Trip anas = trip(ana, "Ana's trip", 0);

    assertThatThrownBy(() -> exampleTripService.findById(anas.getId()))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(
            () -> activityService.findExamplePage(anas.getId(), null, PageRequest.of(0, 10)))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
