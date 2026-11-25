package com.ruben.backend.metrics.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.activity.dto.ActivityDTO;
import com.ruben.backend.activity.dto.ActivityRequest;
import com.ruben.backend.activity.service.ActivityService;
import com.ruben.backend.common.security.TripMateUserDetails;
import com.ruben.backend.common.security.TripMateUserDetailsService;
import com.ruben.backend.metrics.dto.PlatformMetricsDTO;
import com.ruben.backend.trip.dto.TripDTO;
import com.ruben.backend.trip.dto.TripRequest;
import com.ruben.backend.trip.service.TripService;
import com.ruben.backend.user.dto.RegisterRequest;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AdminAccountInitializer;
import com.ruben.backend.user.service.UserService;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@Tag("I6")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MetricsServiceIntegrationTest {

  @Autowired private MetricsService metricsService;
  @Autowired private TripService tripService;
  @Autowired private ActivityService activityService;
  @Autowired private UserService userService;
  @Autowired private UserRepository userRepository;
  @Autowired private TripMateUserDetailsService userDetailsService;
  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private AdminAccountInitializer adminAccountInitializer;

  private UserDTO ana;
  private UserDTO bob;

  @BeforeEach
  void setUp() {
    databaseCleaner.clear();
    adminAccountInitializer.run(new DefaultApplicationArguments());
    ana = userService.register(new RegisterRequest("Ana", "ana@example.com", "secret-pass"));
    bob = userService.register(new RegisterRequest("Bob", "bob@example.com", "secret-pass"));
    logInAs(ana);
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

  private TripDTO createTrip(String name) {
    return tripService.create(
        new TripRequest(
            name, "City", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 14), null, null));
  }

  private ActivityDTO createActivity(Long tripId, String name) {
    return activityService.create(
        tripId,
        new ActivityRequest(name, LocalDate.of(2027, 7, 11), null, null, null, null, null, null));
  }

  @Test
  void anEmptyPlatformHasNoUsersTripsOrActivitiesAndTheAdministratorIsNotCounted() {
    databaseCleaner.clear();
    adminAccountInitializer.run(new DefaultApplicationArguments());

    assertThat(metricsService.getPlatformMetrics()).isEqualTo(new PlatformMetricsDTO(0, 0, 0));
  }

  @Test
  void usersCountTheRegularAccountsBlockedOnesIncludedAndNotTheAdministrator() {
    User blocked = userRepository.findById(bob.id()).orElseThrow();
    blocked.setStatus(UserStatus.BLOCKED);
    userRepository.save(blocked);
    userService.register(new RegisterRequest("Eva", "eva@example.com", "secret-pass"));

    assertThat(metricsService.getPlatformMetrics().users()).isEqualTo(3);
  }

  @Test
  void tripsCountPublishedAndUnpublishedOnesOfEveryCreator() {
    createTrip("Rome");
    TripDTO oslo = createTrip("Oslo");
    tripService.changePublication(oslo.id(), true);
    logInAs(bob);
    createTrip("Lisbon");

    assertThat(metricsService.getPlatformMetrics().trips()).isEqualTo(3);
  }

  @Test
  void activitiesCountEveryActivityOfEveryTrip() {
    TripDTO rome = createTrip("Rome");
    TripDTO oslo = createTrip("Oslo");
    createActivity(rome.id(), "Colosseum");
    createActivity(rome.id(), "Forum");
    createActivity(oslo.id(), "Fjord");

    assertThat(metricsService.getPlatformMetrics().activities()).isEqualTo(3);
  }

  @Test
  void theTotalsFollowCreatingAndDeletingTripsAndActivities() {
    PlatformMetricsDTO start = metricsService.getPlatformMetrics();

    TripDTO rome = createTrip("Rome");
    ActivityDTO colosseum = createActivity(rome.id(), "Colosseum");
    createActivity(rome.id(), "Forum");
    PlatformMetricsDTO created = metricsService.getPlatformMetrics();
    assertThat(created.trips()).isEqualTo(start.trips() + 1);
    assertThat(created.activities()).isEqualTo(start.activities() + 2);

    activityService.delete(rome.id(), colosseum.id());
    assertThat(metricsService.getPlatformMetrics().activities()).isEqualTo(start.activities() + 1);

    tripService.delete(rome.id());
    assertThat(metricsService.getPlatformMetrics())
        .isEqualTo(new PlatformMetricsDTO(start.users(), start.trips(), start.activities()));
  }
}
