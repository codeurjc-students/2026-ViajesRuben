package com.ruben.backend.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.activity.dto.ActivityDTO;
import com.ruben.backend.activity.dto.ActivityRequest;
import com.ruben.backend.activity.dto.TripCostDTO;
import com.ruben.backend.activity.entity.ActivityCategory;
import com.ruben.backend.activity.repository.ActivityRepository;
import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.BadRequestException;
import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.common.security.TripMateUserDetails;
import com.ruben.backend.common.security.TripMateUserDetailsService;
import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.image.repository.ImageRepository;
import com.ruben.backend.trip.dto.TripDTO;
import com.ruben.backend.trip.dto.TripRequest;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.trip.service.TripService;
import com.ruben.backend.user.dto.RegisterRequest;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.service.UserService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@Tag("B5")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ActivityServiceIntegrationTest {

  private static final LocalDate START = LocalDate.of(2027, 7, 10);
  private static final LocalDate END = LocalDate.of(2027, 7, 14);

  @Autowired private ActivityService activityService;
  @Autowired private TripService tripService;
  @Autowired private UserService userService;
  @Autowired private ActivityRepository activityRepository;
  @Autowired private TripRepository tripRepository;
  @Autowired private ImageRepository imageRepository;
  @Autowired private TripMateUserDetailsService userDetailsService;
  @Autowired private DatabaseCleaner databaseCleaner;

  private UserDTO ana;
  private UserDTO bob;
  private TripDTO trip;

  @BeforeEach
  void setUp() {
    databaseCleaner.clear();
    ana = userService.register(new RegisterRequest("Ana", "ana@example.com", "secret-pass"));
    bob = userService.register(new RegisterRequest("Bob", "bob@example.com", "secret-pass"));
    logInAs(ana);
    trip = tripService.create(new TripRequest("Rome", "Rome", START, END, null, null));
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private void logInAs(UserDTO user) {
    TripMateUserDetails details = userDetailsService.loadUserById(user.id());
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                details, null, details.getAuthorities()));
  }

  private ActivityDTO create(String name, int dayOffset, LocalTime start) {
    return activityService.create(
        trip.id(),
        new ActivityRequest(
            name,
            START.plusDays(dayOffset),
            start,
            start == null ? null : start.plusHours(1),
            null,
            null,
            null,
            null));
  }

  private static MockMultipartFile png() {
    return new MockMultipartFile("file", "a.png", "image/png", ImageTestData.png(100));
  }

  private static PageRequest byTime(int page, int size) {
    return PageRequest.of(page, size, Sort.by("date", "startTime", "id"));
  }

  @Test
  void createPersistsEveryFieldWithTheAuthorAndTheTrip() {
    ActivityDTO created =
        activityService.create(
            trip.id(),
            new ActivityRequest(
                " Colosseum ",
                START.plusDays(1),
                LocalTime.of(9, 30),
                LocalTime.of(11, 0),
                "Piazza del Colosseo",
                "Guided tour",
                new BigDecimal("16.5"),
                ActivityCategory.SIGHTSEEING));

    var stored = activityRepository.findById(created.id()).orElseThrow();
    assertThat(stored.getTrip().getId()).isEqualTo(trip.id());
    assertThat(stored.getAuthor().getId()).isEqualTo(ana.id());
    assertThat(stored.getName()).isEqualTo("Colosseum");
    assertThat(stored.getDate()).isEqualTo(START.plusDays(1));
    assertThat(stored.getStartTime()).isEqualTo(LocalTime.of(9, 30));
    assertThat(stored.getEndTime()).isEqualTo(LocalTime.of(11, 0));
    assertThat(stored.getCost()).isEqualByComparingTo("16.50");
    assertThat(stored.getCategory()).isEqualTo(ActivityCategory.SIGHTSEEING);
    assertThat(stored.getCreatedAt()).isNotNull();
    assertThat(stored.getImage()).isNull();
  }

  @Test
  void aDateOutsideTheTripIsRejected() {
    assertThatThrownBy(() -> create("Early", -1, null)).isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> create("Late", 5, null)).isInstanceOf(BadRequestException.class);
    assertThat(activityRepository.count()).isZero();
  }

  @Test
  void filteringByDayReturnsOnlyThatDaySortedByStartTimeWithAllDayFirst() {
    create("Dinner", 1, LocalTime.of(20, 0));
    create("Museum", 1, LocalTime.of(10, 0));
    create("Market", 1, null);
    create("Other day", 2, LocalTime.of(8, 0));

    PageDTO<ActivityDTO> day =
        activityService.findPage(trip.id(), START.plusDays(1), byTime(0, 10));

    assertThat(day.content())
        .extracting(ActivityDTO::name)
        .containsExactly("Market", "Museum", "Dinner");
    assertThat(day.totalElements()).isEqualTo(3);
  }

  @Test
  void withoutADayTheWholeItineraryIsPagedByDateAndTime() {
    create("C", 2, LocalTime.of(9, 0));
    create("A", 0, LocalTime.of(9, 0));
    create("B", 1, LocalTime.of(9, 0));

    PageDTO<ActivityDTO> first = activityService.findPage(trip.id(), null, byTime(0, 2));
    PageDTO<ActivityDTO> second = activityService.findPage(trip.id(), null, byTime(1, 2));

    assertThat(first.content()).extracting(ActivityDTO::name).containsExactly("A", "B");
    assertThat(first.last()).isFalse();
    assertThat(second.content()).extracting(ActivityDTO::name).containsExactly("C");
    assertThat(second.last()).isTrue();
  }

  @Test
  void anActivityIsOnlyFoundThroughItsOwnTrip() {
    ActivityDTO created = create("Museum", 1, null);
    TripDTO other = tripService.create(new TripRequest("Oslo", "Oslo", START, END, null, null));

    assertThat(activityService.findById(trip.id(), created.id()).name()).isEqualTo("Museum");
    assertThatThrownBy(() -> activityService.findById(other.id(), created.id()))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThat(activityService.findPage(other.id(), null, byTime(0, 10)).content()).isEmpty();
  }

  @Test
  void anotherUsersTripAndItsActivitiesAreOffLimits() {
    ActivityDTO created = create("Museum", 1, null);
    logInAs(bob);

    assertThatThrownBy(() -> activityService.findPage(trip.id(), null, byTime(0, 10)))
        .isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> activityService.findById(trip.id(), created.id()))
        .isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> create("Mine", 1, null)).isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> activityService.delete(trip.id(), created.id()))
        .isInstanceOf(ForbiddenException.class);
    assertThat(activityRepository.count()).isEqualTo(1);
  }

  @Test
  void updateReplacesTheFieldsAndKeepsTheImage() {
    ActivityDTO created = create("Old", 1, LocalTime.of(9, 0));
    activityService.replaceImage(trip.id(), created.id(), png());

    ActivityDTO updated =
        activityService.update(
            trip.id(),
            created.id(),
            new ActivityRequest("New", START.plusDays(2), null, null, "Forum", null, null, null));

    assertThat(updated.name()).isEqualTo("New");
    assertThat(updated.date()).isEqualTo(START.plusDays(2));
    assertThat(updated.startTime()).isNull();
    assertThat(updated.location()).isEqualTo("Forum");
    assertThat(updated.imageId()).isNotNull();
  }

  @Test
  void deleteRemovesTheActivityAndItsImage() {
    ActivityDTO created = create("Museum", 1, null);
    activityService.replaceImage(trip.id(), created.id(), png());
    assertThat(imageRepository.count()).isEqualTo(1);

    activityService.delete(trip.id(), created.id());

    assertThat(activityRepository.count()).isZero();
    assertThat(imageRepository.count()).isZero();
  }

  @Test
  void replacingAndRemovingTheImageLeavesNoOrphanImage() {
    ActivityDTO created = create("Museum", 1, null);

    assertThat(activityService.replaceImage(trip.id(), created.id(), png()).created()).isTrue();
    assertThat(activityService.replaceImage(trip.id(), created.id(), png()).created()).isFalse();
    assertThat(imageRepository.count()).isEqualTo(1);
    assertThat(activityService.findImageContent(trip.id(), created.id()).contentType())
        .isEqualTo("image/png");

    activityService.deleteImage(trip.id(), created.id());

    assertThat(imageRepository.count()).isZero();
    assertThatThrownBy(() -> activityService.findImageContent(trip.id(), created.id()))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void deletingATripRemovesItsActivitiesTheirImagesAndTheCover() {
    ActivityDTO withImage = create("Museum", 1, null);
    ActivityDTO withoutImage = create("Walk", 2, null);
    activityService.replaceImage(trip.id(), withImage.id(), png());
    tripService.replaceCover(trip.id(), png());
    TripDTO other = tripService.create(new TripRequest("Oslo", "Oslo", START, END, null, null));
    ActivityDTO kept =
        activityService.create(
            other.id(), new ActivityRequest("Fjords", START, null, null, null, null, null, null));
    activityService.replaceImage(other.id(), kept.id(), png());
    assertThat(imageRepository.count()).isEqualTo(3);

    tripService.delete(trip.id());

    assertThat(tripRepository.findById(trip.id())).isEmpty();
    assertThat(activityRepository.findById(withImage.id())).isEmpty();
    assertThat(activityRepository.findById(withoutImage.id())).isEmpty();
    assertThat(activityRepository.findById(kept.id())).isPresent();
    assertThat(imageRepository.count()).isEqualTo(1);
  }

  @Test
  void tripDatesMayLeaveAnActivityOutsideAndItCanThenBeMovedInside() {
    ActivityDTO created = create("Second day", 1, null);
    TripRequest shorter = new TripRequest("Rome", "Rome", START.plusDays(2), END, null, null);

    assertThat(tripService.update(trip.id(), shorter).startDate()).isEqualTo(START.plusDays(2));
    assertThat(activityRepository.findById(created.id())).isPresent();

    assertThatThrownBy(
            () ->
                activityService.update(
                    trip.id(),
                    created.id(),
                    new ActivityRequest("Second day", START, null, null, null, null, null, null)))
        .isInstanceOf(BadRequestException.class);
    activityService.update(
        trip.id(),
        created.id(),
        new ActivityRequest("Second day", END, null, null, null, null, null, null));
    assertThat(activityRepository.findOutsideTrip(trip.id())).isEmpty();
  }

  @Test
  void aTripCanBeRenamedWithoutLookingAtItsActivities() {
    create("Second day", 1, null);

    TripDTO renamed =
        tripService.update(trip.id(), new TripRequest("Roma", "Rome", START, END, null, null));

    assertThat(renamed.name()).isEqualTo("Roma");
  }

  @Test
  void findCostSumsTheCostsInTheDatabaseAndSubtractsThemFromTheBudget() {
    TripDTO budgeted =
        tripService.create(
            new TripRequest("Rome", "Rome", START, END, new BigDecimal("100.00"), null));
    for (String cost : new String[] {"30.50", "20.25"}) {
      activityService.create(
          budgeted.id(),
          new ActivityRequest(
              "Costly " + cost, START, null, null, null, null, new BigDecimal(cost), null));
    }
    activityService.create(
        budgeted.id(), new ActivityRequest("Free", START, null, null, null, null, null, null));
    activityService.create(
        trip.id(),
        new ActivityRequest(
            "Elsewhere", START, null, null, null, null, new BigDecimal("999"), null));

    TripCostDTO cost = activityService.findCost(budgeted.id());

    assertThat(cost.totalCost()).isEqualByComparingTo("50.75");
    assertThat(cost.remainingBudget()).isEqualByComparingTo("49.25");
  }

  @Test
  void findCostOfATripWithoutCostsOrBudgetIsZeroWithNoRemainingBudget() {
    TripCostDTO cost = activityService.findCost(trip.id());

    assertThat(cost.totalCost()).isEqualByComparingTo("0");
    assertThat(cost.remainingBudget()).isNull();
  }
}
