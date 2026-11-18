package com.ruben.backend.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.activity.repository.ActivityRepository;
import com.ruben.backend.common.security.AdminProperties;
import com.ruben.backend.image.repository.ImageRepository;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.dto.LoginRequest;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AdminAccountInitializer;
import com.ruben.backend.user.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

/** The sample data is off in the tests, so the loader is built by hand over the real database. */
@Tag("B4")
@Tag("B5")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DataLoaderIntegrationTest {

  @Autowired private UserRepository userRepository;
  @Autowired private TripRepository tripRepository;
  @Autowired private ActivityRepository activityRepository;
  @Autowired private ImageRepository imageRepository;
  @Autowired private AuthService authService;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AdminProperties adminProperties;
  @Autowired private AdminAccountInitializer adminAccountInitializer;
  @Autowired private DatabaseCleaner databaseCleaner;

  private DataLoader dataLoader;

  @BeforeEach
  void setUp() {
    databaseCleaner.clear();
    adminAccountInitializer.run(new DefaultApplicationArguments());
    dataLoader =
        new DataLoader(
            userRepository,
            tripRepository,
            activityRepository,
            imageRepository,
            passwordEncoder,
            adminProperties,
            "Demo-2026");
  }

  @Test
  void loadsTheSampleUsersWithTheirTripsAndRunningAgainAddsNothing() {
    dataLoader.run();
    dataLoader.run();

    // Three sample users and the administrator; 17 trips of the users and the 3 example trips.
    assertThat(userRepository.count()).isEqualTo(4);
    assertThat(tripRepository.count()).isEqualTo(20);
    assertThat(
            tripRepository
                .findByCreatorId(
                    userRepository.findByEmail("ana@tripmate.com").orElseThrow().getId(),
                    PageRequest.of(0, 50))
                .getTotalElements())
        .isEqualTo(12);
  }

  @Test
  @Tag("B1")
  void theAdministratorOwnsTheExampleTripsWithTheirCoversAndAStoppedCoverIsRestored() {
    dataLoader.run();
    var admin = userRepository.findByEmail(adminProperties.email()).orElseThrow();

    var examples =
        tripRepository.findByCreatorId(
            admin.getId(), PageRequest.of(0, 10, Sort.by("startDate", "id")));

    assertThat(examples.getTotalElements()).isEqualTo(3);
    assertThat(examples.getContent())
        .allSatisfy(
            trip -> {
              assertThat(trip.getCreator().getId()).isEqualTo(admin.getId());
              assertThat(trip.getCoverImage()).isNotNull();
              assertThat(trip.getBudget()).isNotNull();
            });
    assertThat(
            tripRepository.findAll().stream()
                .filter(trip -> !trip.getCreator().getId().equals(admin.getId()))
                .map(trip -> trip.getCoverImage())
                .filter(java.util.Objects::nonNull))
        .isEmpty();

    var rome = examples.getContent().get(0);
    rome.setCoverImage(null);
    tripRepository.save(rome);
    dataLoader.run();

    assertThat(tripRepository.findById(rome.getId()).orElseThrow().getCoverImage()).isNotNull();
  }

  @Test
  @Tag("I7")
  void theSampleTripsAreCreatedPublishedByThreeAuthorsNewestFirst() {
    dataLoader.run();

    var published =
        tripRepository.searchPublished(
            null, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "publishedAt", "id")));

    assertThat(published.getTotalElements()).isEqualTo(5);
    assertThat(published.getContent())
        .extracting(Trip::getName)
        .containsExactly(
            "Roma en cinco días",
            "Escapada a las Maldivas",
            "Lo mejor de Tokio",
            "Lisbon getaway",
            "Andalusia route");
    assertThat(published.getContent())
        .extracting(trip -> trip.getCreator().getId())
        .doesNotContainNull()
        .hasSize(5)
        .satisfies(ids -> assertThat(ids.stream().distinct()).hasSize(3));
  }

  @Test
  @Tag("I7")
  void aSampleTripSomebodyUnpublishedStaysUnpublishedAfterARestart() {
    dataLoader.run();
    var admin = userRepository.findByEmail(adminProperties.email()).orElseThrow();
    Trip rome =
        tripRepository
            .findByCreatorIdAndNameIn(admin.getId(), java.util.List.of("Roma en cinco días"))
            .get(0);
    rome.unpublish();
    tripRepository.save(rome);

    dataLoader.run();

    Trip reloaded = tripRepository.findById(rome.getId()).orElseThrow();
    assertThat(reloaded.isPublished()).isFalse();
    assertThat(reloaded.getPublishedAt()).isNull();
    assertThat(tripRepository.searchPublished(null, PageRequest.of(0, 10)).getTotalElements())
        .isEqualTo(4);
  }

  @Test
  void theSampleUsersLogInWithTheDocumentedPassword() {
    dataLoader.run();

    for (String email :
        new String[] {"ana@tripmate.com", "carlos@tripmate.com", "laura@tripmate.com"}) {
      assertThat(authService.login(new LoginRequest(email, "Demo-2026")).user().email())
          .isEqualTo(email);
    }
  }

  @Test
  void aDifferentConfiguredPasswordIsTheOneTheSampleUsersLogInWith() {
    new DataLoader(
            userRepository,
            tripRepository,
            activityRepository,
            imageRepository,
            passwordEncoder,
            adminProperties,
            "Other-2026")
        .run();

    assertThat(authService.login(new LoginRequest("ana@tripmate.com", "Other-2026")).user().email())
        .isEqualTo("ana@tripmate.com");
    LoginRequest withTheDefaultPassword = new LoginRequest("ana@tripmate.com", "Demo-2026");
    assertThatThrownBy(() -> authService.login(withTheDefaultPassword))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void loadsTheSampleItinerariesWithTheirImagesAndRunningAgainAddsNothing() {
    dataLoader.run();
    long loaded = activityRepository.count();
    long images = imageRepository.count();

    dataLoader.run();

    // 18 activities and 4 pictures of the users, 40 activities and 3 covers of the examples.
    assertThat(loaded).isEqualTo(58);
    assertThat(images).isEqualTo(7);
    assertThat(activityRepository.count()).isEqualTo(loaded);
    assertThat(imageRepository.count()).isEqualTo(images);
    var lisbon =
        tripRepository
            .findByCreatorIdAndNameIn(
                userRepository.findByEmail("ana@tripmate.com").orElseThrow().getId(),
                java.util.List.of("Lisbon getaway"))
            .get(0);
    assertThat(
            activityRepository
                .findByTrip(
                    lisbon.getId(),
                    lisbon.getStartDate(),
                    PageRequest.of(0, 10, Sort.by("startTime")))
                .getContent())
        .extracting(a -> a.getName())
        .containsExactly("Alfama walking tour", "Lunch at Time Out Market");
  }
}
