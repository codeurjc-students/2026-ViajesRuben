package com.ruben.backend.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.activity.dto.ActivityRequest;
import com.ruben.backend.activity.dto.CategorySpendingDTO;
import com.ruben.backend.activity.dto.DaySpendingDTO;
import com.ruben.backend.activity.dto.TripSpendingDTO;
import com.ruben.backend.activity.entity.ActivityCategory;
import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.common.security.TripMateUserDetails;
import com.ruben.backend.common.security.TripMateUserDetailsService;
import com.ruben.backend.invitation.dto.InvitationRequest;
import com.ruben.backend.invitation.entity.InvitationStatus;
import com.ruben.backend.invitation.service.InvitationService;
import com.ruben.backend.trip.dto.TripRequest;
import com.ruben.backend.trip.service.TripService;
import com.ruben.backend.user.dto.RegisterRequest;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.service.UserService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** I5: the two GROUP BY queries and the zero days, against a real MySQL. */
@Tag("I5")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ActivitySpendingIntegrationTest {

  private static final LocalDate START = LocalDate.of(2027, 7, 10);
  private static final LocalDate END = LocalDate.of(2027, 7, 14);

  @Autowired private ActivityService activityService;
  @Autowired private TripService tripService;
  @Autowired private UserService userService;
  @Autowired private InvitationService invitationService;
  @Autowired private TripMateUserDetailsService userDetailsService;
  @Autowired private DatabaseCleaner databaseCleaner;

  private UserDTO ana;
  private UserDTO bob;
  private UserDTO carla;
  private long tripId;

  @BeforeEach
  void setUp() {
    databaseCleaner.clear();
    ana = userService.register(new RegisterRequest("Ana", "ana@example.com", "secret-pass"));
    bob = userService.register(new RegisterRequest("Bob", "bob@example.com", "secret-pass"));
    carla = userService.register(new RegisterRequest("Carla", "carla@example.com", "secret-pass"));
    logInAs(ana);
    tripId = newTrip(new BigDecimal("200.00"));
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

  private long newTrip(BigDecimal budget) {
    return tripService.create(new TripRequest("Rome", "Rome", START, END, budget, null)).id();
  }

  private void add(long trip, int dayOffset, String cost, ActivityCategory category) {
    activityService.create(
        trip,
        new ActivityRequest(
            "Activity",
            START.plusDays(dayOffset),
            null,
            null,
            null,
            null,
            cost == null ? null : new BigDecimal(cost),
            category));
  }

  @Test
  void sumsPerCategoryAndPerDayIgnoringActivitiesWithoutCostAndGroupingTheWithoutCategory() {
    add(tripId, 0, "20.00", ActivityCategory.FOOD);
    add(tripId, 0, "15.50", ActivityCategory.FOOD);
    add(tripId, 2, "100.00", ActivityCategory.ACCOMMODATION);
    add(tripId, 2, "4.50", null);
    add(tripId, 3, "10.00", null);
    add(tripId, 3, null, ActivityCategory.TRANSPORT);
    add(tripId, 4, null, null);

    TripSpendingDTO spending = activityService.findSpending(tripId);

    assertThat(spending.budget()).isEqualByComparingTo("200.00");
    assertThat(spending.totalCost()).isEqualByComparingTo("150.00");
    assertThat(spending.byCategory())
        .extracting(CategorySpendingDTO::category)
        .containsExactly(ActivityCategory.ACCOMMODATION, ActivityCategory.FOOD, null);
    assertThat(spending.byCategory())
        .extracting(c -> c.total().toPlainString())
        .containsExactly("100.00", "35.50", "14.50");
    assertThat(spending.byDay()).hasSize(5);
    assertThat(spending.byDay())
        .extracting(d -> d.total().compareTo(BigDecimal.ZERO) == 0 ? "0" : d.total().toString())
        .containsExactly("35.50", "0", "104.50", "10.00", "0");
    assertThat(spending.byDay())
        .extracting(d -> d.cumulative().toString())
        .containsExactly("35.50", "35.50", "140.00", "150.00", "150.00");
    assertThat(spending.byDay().get(4).cumulative()).isEqualByComparingTo(spending.totalCost());
  }

  @Test
  void aCostOfZeroCountsAsACostedCategoryButAddsNothing() {
    add(tripId, 1, "0.00", ActivityCategory.LEISURE);

    TripSpendingDTO spending = activityService.findSpending(tripId);

    assertThat(spending.totalCost()).isEqualByComparingTo("0");
    assertThat(spending.byCategory())
        .extracting(CategorySpendingDTO::category)
        .containsExactly(ActivityCategory.LEISURE);
  }

  @Test
  void aTripWithoutCostsHasNoCategoriesAndEveryDayAtZero() {
    add(tripId, 0, null, ActivityCategory.FOOD);

    TripSpendingDTO spending = activityService.findSpending(tripId);

    assertThat(spending.totalCost()).isEqualByComparingTo("0");
    assertThat(spending.byCategory()).isEmpty();
    assertThat(spending.byDay()).hasSize(5);
    assertThat(spending.byDay())
        .allSatisfy(d -> assertThat(d.cumulative()).isEqualByComparingTo("0"));
  }

  @Test
  void aTripWithoutBudgetHasANullBudget() {
    long noBudget = newTrip(null);
    add(noBudget, 1, "12.00", ActivityCategory.FOOD);

    TripSpendingDTO spending = activityService.findSpending(noBudget);

    assertThat(spending.budget()).isNull();
    assertThat(spending.totalCost()).isEqualByComparingTo("12.00");
  }

  @Test
  void theCostsOfAnotherTripAreNotMixedIn() {
    long other = newTrip(null);
    add(other, 0, "999.00", ActivityCategory.FOOD);
    add(tripId, 0, "1.00", ActivityCategory.FOOD);

    TripSpendingDTO spending = activityService.findSpending(tripId);

    assertThat(spending.totalCost()).isEqualByComparingTo("1.00");
    assertThat(spending.byCategory()).hasSize(1);
    assertThat(spending.byCategory().get(0).total()).isEqualByComparingTo("1.00");
  }

  @Test
  void anActivityOutsideTheTripDatesStillCountsAndEndsTheLine() {
    add(tripId, 0, "10.00", ActivityCategory.FOOD);
    activityService.create(
        tripId,
        new ActivityRequest(
            "Inside", START.plusDays(1), null, null, null, null, new BigDecimal("5.00"), null));
    // The trip is shortened afterwards, which leaves the last days' activities outside its dates.
    tripService.update(tripId, new TripRequest("Rome", "Rome", START, START, null, null));

    TripSpendingDTO spending = activityService.findSpending(tripId);

    assertThat(spending.byDay())
        .extracting(DaySpendingDTO::date)
        .containsExactly(START, START.plusDays(1));
    assertThat(spending.byDay().get(1).cumulative()).isEqualByComparingTo("15.00");
    assertThat(spending.totalCost()).isEqualByComparingTo("15.00");
  }

  @Test
  void theParticipantReadsItButAStrangerDoesNotAndAMissingTripIsNotFound() {
    add(tripId, 0, "10.00", ActivityCategory.FOOD);
    long invitation = invitationService.create(tripId, new InvitationRequest(bob.email())).id();
    logInAs(bob);
    invitationService.respond(invitation, InvitationStatus.ACCEPTED);

    assertThat(activityService.findSpending(tripId).totalCost()).isEqualByComparingTo("10.00");

    logInAs(carla);
    assertThatThrownBy(() -> activityService.findSpending(tripId))
        .isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> activityService.findSpending(999_999L))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
