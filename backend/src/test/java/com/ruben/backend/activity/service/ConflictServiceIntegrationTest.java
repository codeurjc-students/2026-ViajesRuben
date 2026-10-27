package com.ruben.backend.activity.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.activity.dto.ActivityDTO;
import com.ruben.backend.activity.dto.ActivityRequest;
import com.ruben.backend.activity.dto.TripConflictsDTO;
import com.ruben.backend.common.security.TripMateUserDetails;
import com.ruben.backend.common.security.TripMateUserDetailsService;
import com.ruben.backend.trip.dto.TripDTO;
import com.ruben.backend.trip.dto.TripRequest;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@Tag("B6")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ConflictServiceIntegrationTest {

  private static final LocalDate START = LocalDate.of(2027, 7, 10);
  private static final LocalDate END = LocalDate.of(2027, 7, 14);

  @Autowired private ConflictService conflictService;
  @Autowired private ActivityService activityService;
  @Autowired private TripService tripService;
  @Autowired private UserService userService;
  @Autowired private TripMateUserDetailsService userDetailsService;
  @Autowired private DatabaseCleaner databaseCleaner;

  private TripDTO trip;

  @BeforeEach
  void setUp() {
    databaseCleaner.clear();
    UserDTO ana =
        userService.register(new RegisterRequest("Ana", "ana@example.com", "secret-pass"));
    TripMateUserDetails details = userDetailsService.loadUserById(ana.id());
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                details, null, details.getAuthorities()));
    trip = createTrip(null);
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private TripDTO createTrip(BigDecimal budget) {
    return tripService.create(new TripRequest("Rome", "Rome", START, END, budget, null));
  }

  private ActivityDTO activity(
      String name, int dayOffset, LocalTime start, LocalTime end, String cost) {
    return activityService.create(
        trip.id(),
        new ActivityRequest(
            name,
            START.plusDays(dayOffset),
            start,
            end,
            null,
            null,
            cost == null ? null : new BigDecimal(cost),
            null));
  }

  private static LocalTime at(int hour, int minute) {
    return LocalTime.of(hour, minute);
  }

  private TripConflictsDTO conflicts() {
    return conflictService.findConflicts(trip.id());
  }

  // --- overlaps ---

  @Test
  void aTripWithoutActivitiesHasNoConflicts() {
    TripConflictsDTO conflicts = conflicts();

    assertThat(conflicts.overlaps()).isEmpty();
    assertThat(conflicts.outsideDates()).isEmpty();
    assertThat(conflicts.budget()).isNull();
  }

  @Test
  void twoActivitiesWhoseTimesIntersectOverlapAndThePairIsListedOnce() {
    ActivityDTO tour = activity("Tour", 0, at(10, 0), at(12, 0), null);
    ActivityDTO lunch = activity("Lunch", 0, at(11, 0), at(13, 0), null);

    assertThat(conflicts().overlaps())
        .singleElement()
        .satisfies(
            overlap -> {
              assertThat(overlap.date()).isEqualTo(START);
              assertThat(overlap.firstId()).isEqualTo(tour.id());
              assertThat(overlap.firstName()).isEqualTo("Tour");
              assertThat(overlap.firstStart()).isEqualTo(at(10, 0));
              assertThat(overlap.firstEnd()).isEqualTo(at(12, 0));
              assertThat(overlap.secondId()).isEqualTo(lunch.id());
              assertThat(overlap.secondName()).isEqualTo("Lunch");
              assertThat(overlap.secondStart()).isEqualTo(at(11, 0));
              assertThat(overlap.secondEnd()).isEqualTo(at(13, 0));
            });
  }

  @Test
  void anActivityInsideAnotherOverlapsIt() {
    activity("Long", 0, at(9, 0), at(18, 0), null);
    activity("Short", 0, at(12, 0), at(13, 0), null);

    assertThat(conflicts().overlaps()).hasSize(1);
  }

  @Test
  void timesThatOnlyTouchDoNotOverlap() {
    activity("Morning", 0, at(10, 0), at(11, 0), null);
    activity("Next", 0, at(11, 0), at(12, 0), null);

    assertThat(conflicts().overlaps()).isEmpty();
  }

  @Test
  void intersectingTimesOnDifferentDaysDoNotOverlap() {
    activity("Day one", 0, at(10, 0), at(12, 0), null);
    activity("Day two", 1, at(10, 0), at(12, 0), null);

    assertThat(conflicts().overlaps()).isEmpty();
  }

  @Test
  void activitiesWithoutStartAndEndTimeAreIgnored() {
    activity("All day", 0, null, null, null);
    activity("Tour", 0, at(10, 0), at(12, 0), null);
    activity("Another all day", 0, null, null, null);

    assertThat(conflicts().overlaps()).isEmpty();
  }

  @Test
  void threeMutuallyOverlappingActivitiesGiveThreePairsInTimeOrder() {
    activity("A", 0, at(10, 0), at(14, 0), null);
    activity("B", 0, at(11, 0), at(14, 0), null);
    activity("C", 0, at(12, 0), at(14, 0), null);

    assertThat(conflicts().overlaps())
        .extracting(overlap -> overlap.firstName() + overlap.secondName())
        .containsExactly("AB", "AC", "BC");
  }

  @Test
  void overlapsOfAnotherTripAreNotReported() {
    TripDTO other = createTrip(null);
    activity("Tour", 0, at(10, 0), at(12, 0), null);
    activityService.create(
        other.id(),
        new ActivityRequest("Lunch", START, at(11, 0), at(13, 0), null, null, null, null));

    assertThat(conflicts().overlaps()).isEmpty();
    assertThat(conflictService.findConflicts(other.id()).overlaps()).isEmpty();
  }

  // --- outside the trip's dates ---

  @Test
  void activitiesInsideTheTripDatesAreNotOutside() {
    activity("First day", 0, null, null, null);
    activity("Last day", 4, null, null, null);

    assertThat(conflicts().outsideDates()).isEmpty();
  }

  @Test
  void shorteningTheTripLeavesActivitiesOutsideAndTheyAreListedInOrder() {
    ActivityDTO first = activity("First day", 0, null, null, null);
    ActivityDTO fourth = activity("Fourth day", 3, null, null, null);
    ActivityDTO last = activity("Last day", 4, null, null, null);

    tripService.update(
        trip.id(),
        new TripRequest("Rome", "Rome", START.plusDays(1), END.minusDays(2), null, null));

    assertThat(conflicts().outsideDates())
        .extracting(outside -> outside.activityId() + ":" + outside.date())
        .containsExactly(
            first.id() + ":" + START, fourth.id() + ":" + START.plusDays(3), last.id() + ":" + END);
    assertThat(conflicts().outsideDates().get(0).name()).isEqualTo("First day");
  }

  // --- budget ---

  @Test
  void aTotalAboveTheBudgetIsAConflictWithBothAmounts() {
    trip = createTrip(new BigDecimal("100.00"));
    activity("Tour", 0, null, null, "60.50");
    activity("Dinner", 1, null, null, "40.51");

    assertThat(conflicts().budget())
        .satisfies(
            budget -> {
              assertThat(budget.totalCost()).isEqualByComparingTo("101.01");
              assertThat(budget.budget()).isEqualByComparingTo("100.00");
            });
  }

  @Test
  void aTotalEqualToTheBudgetIsNotAConflict() {
    trip = createTrip(new BigDecimal("100.00"));
    activity("Tour", 0, null, null, "60.00");
    activity("Dinner", 1, null, null, "40.00");

    assertThat(conflicts().budget()).isNull();
  }

  @Test
  void activitiesWithoutCostAddNothing() {
    trip = createTrip(new BigDecimal("50.00"));
    activity("Free walk", 0, null, null, null);
    activity("Tour", 1, null, null, "49.99");

    assertThat(conflicts().budget()).isNull();
  }

  @Test
  void aTripWithoutBudgetNeverHasABudgetConflict() {
    activity("Expensive", 0, null, null, "99999.00");

    assertThat(conflicts().budget()).isNull();
  }

  @Test
  void aBudgetOfAnotherTripDoesNotCountTheseCosts() {
    trip = createTrip(new BigDecimal("10.00"));
    TripDTO other = createTrip(null);
    activityService.create(
        other.id(),
        new ActivityRequest(
            "Expensive", START, null, null, null, null, new BigDecimal("500.00"), null));

    assertThat(conflicts().budget()).isNull();
  }
}
