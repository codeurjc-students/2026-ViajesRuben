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
import com.ruben.backend.trip.dto.PublicTripDTO;
import com.ruben.backend.trip.dto.PublicTripSummaryDTO;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.repository.UserRepository;
import java.time.Instant;
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

/** No user is logged in: the public trips are public, so none of these calls needs one. */
@Tag("B1")
@Tag("I7")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PublicTripServiceIntegrationTest {

  private static final LocalDate START = LocalDate.of(2027, 7, 10);
  private static final Instant FIRST = Instant.parse("2027-01-01T10:00:00Z");

  @Autowired private PublicTripService publicTripService;
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
    ana = userRepository.save(new User("Ana García", "ana@example.com", "hash", Role.USER));
  }

  /** A trip that was published `minutes` after the first publication of the test. */
  private Trip publishedTrip(User creator, String name, int minutes) {
    Trip trip = trip(creator, name, minutes);
    trip.publish(FIRST.plusSeconds(minutes * 60L));
    return tripRepository.save(trip);
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

  private static PageRequest newestFirst(int page, int size) {
    return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt", "id"));
  }

  @Test
  void findPageListsOnlyPublishedTripsOfAnyUserNewestFirstAndPagesThem() {
    publishedTrip(admin, "Oldest", 1);
    publishedTrip(ana, "Second", 2);
    publishedTrip(admin, "Third", 3);
    publishedTrip(ana, "Newest", 4);
    trip(ana, "Never published", 5);
    trip(admin, "Admin draft", 6);

    PageDTO<PublicTripSummaryDTO> first = publicTripService.findPage(newestFirst(0, 3));
    PageDTO<PublicTripSummaryDTO> second = publicTripService.findPage(newestFirst(1, 3));

    assertThat(first.totalElements()).isEqualTo(4);
    assertThat(first.content())
        .extracting(summary -> summary.trip().name())
        .containsExactly("Newest", "Third", "Second");
    assertThat(first.last()).isFalse();
    assertThat(second.content())
        .extracting(summary -> summary.trip().name())
        .containsExactly("Oldest");
    assertThat(second.last()).isTrue();
  }

  @Test
  void theListShowsTheAuthorsNameOfEveryTrip() {
    publishedTrip(admin, "By the admin", 1);
    publishedTrip(ana, "By Ana", 2);

    PageDTO<PublicTripSummaryDTO> page = publicTripService.findPage(newestFirst(0, 10));

    assertThat(page.content())
        .extracting(summary -> summary.trip().authorName())
        .containsExactly("Ana García", "Admin");
  }

  @Test
  void unpublishingRemovesTheTripFromTheListAndFromEveryPublicRead() {
    Trip rome = publishedTrip(ana, "Rome", 1);
    activity(rome, 0, "Colosseum", LocalTime.of(9, 0));
    assertThat(publicTripService.findPage(newestFirst(0, 10)).totalElements()).isEqualTo(1);

    rome.unpublish();
    tripRepository.save(rome);

    assertThat(publicTripService.findPage(newestFirst(0, 10)).totalElements()).isZero();
    assertThatThrownBy(() -> publicTripService.findById(rome.getId()))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(
            () -> activityService.findPublicPage(rome.getId(), null, PageRequest.of(0, 10)))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void theFirstDaysShowTheFirstActivityOfEachOfTheFirstThreeDays() {
    Trip rome = publishedTrip(ana, "Rome", 0);
    activity(rome, 0, "Lunch", LocalTime.of(13, 0));
    activity(rome, 0, "Colosseum", LocalTime.of(9, 0));
    activity(rome, 2, "Vatican", LocalTime.of(10, 0));
    activity(rome, 3, "Fourth day", LocalTime.of(10, 0));
    // Day 2 (the second) has no activities and is left out.

    PublicTripSummaryDTO summary = publicTripService.findPage(newestFirst(0, 3)).content().get(0);

    assertThat(summary.firstDays())
        .containsExactly(new DayPreviewDTO(1, "Colosseum"), new DayPreviewDTO(3, "Vatican"));
  }

  @Test
  void thePreviewsOfAPageDoNotMixTheTripsAndIgnoreActivitiesOfUnpublishedTrips() {
    Trip rome = publishedTrip(admin, "Rome", 1);
    Trip paris = publishedTrip(ana, "Paris", 2);
    Trip draft = trip(ana, "Draft", 0);
    activity(rome, 0, "Colosseum", LocalTime.of(9, 0));
    activity(paris, 1, "Louvre", LocalTime.of(9, 0));
    activity(draft, 0, "Private", LocalTime.of(9, 0));

    PageDTO<PublicTripSummaryDTO> page = publicTripService.findPage(newestFirst(0, 3));

    assertThat(page.content()).hasSize(2);
    assertThat(page.content().get(0).firstDays()).containsExactly(new DayPreviewDTO(2, "Louvre"));
    assertThat(page.content().get(1).firstDays())
        .containsExactly(new DayPreviewDTO(1, "Colosseum"));
  }

  @Test
  void findByIdAndTheActivitiesOfAPublishedTripAreReadableWithoutLogin() {
    Trip rome = publishedTrip(ana, "Rome", 0);
    activity(rome, 0, "Colosseum", LocalTime.of(9, 0));
    activity(rome, 1, "Vatican", LocalTime.of(9, 0));

    PublicTripDTO found = publicTripService.findById(rome.getId());
    PageDTO<ActivityDTO> day =
        activityService.findPublicPage(
            rome.getId(), START.plusDays(1), PageRequest.of(0, 10, Sort.by("date", "id")));

    assertThat(found.name()).isEqualTo("Rome");
    assertThat(found.authorName()).isEqualTo("Ana García");
    assertThat(day.content()).extracting(ActivityDTO::name).containsExactly("Vatican");
  }

  @Test
  void anUnpublishedTripIsNotFoundAsPublicNeverForbidden() {
    Trip draft = trip(ana, "Draft", 0);

    assertThatThrownBy(() -> publicTripService.findById(draft.getId()))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(
            () -> activityService.findPublicPage(draft.getId(), null, PageRequest.of(0, 10)))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> publicTripService.findById(999_999L))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
