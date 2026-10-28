package com.ruben.backend.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruben.backend.activity.entity.Activity;
import com.ruben.backend.activity.repository.ActivityRepository;
import com.ruben.backend.common.security.AdminProperties;
import com.ruben.backend.image.entity.Image;
import com.ruben.backend.image.repository.ImageRepository;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import com.ruben.backend.user.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("B1")
@Tag("B4")
@Tag("B5")
@ExtendWith(MockitoExtension.class)
class DataLoaderUnitTest {

  private static final AdminProperties ADMIN =
      new AdminProperties("admin@tripmate.com", "Admin", "$2a$10$" + "x".repeat(53));

  @Mock private UserRepository userRepository;
  @Mock private TripRepository tripRepository;
  @Mock private ActivityRepository activityRepository;
  @Mock private ImageRepository imageRepository;
  @Mock private PasswordEncoder passwordEncoder;
  private DataLoader dataLoader;
  @Captor private ArgumentCaptor<User> savedUser;
  @Captor private ArgumentCaptor<Iterable<Trip>> savedTrips;

  @BeforeEach
  void setUp() {
    lenient().when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
    dataLoader =
        new DataLoader(
            userRepository,
            tripRepository,
            activityRepository,
            imageRepository,
            passwordEncoder,
            ADMIN,
            "Demo-2026");
  }

  private static User withId(User user, long id) {
    return UserTestData.user(id, user.getEmail(), Role.USER, UserStatus.ACTIVE);
  }

  @Test
  void theDefaultPasswordOfTheConfigurationIsTheOneDocumentedInTheReadme() throws IOException {
    String properties = Files.readString(Path.of("src/main/resources/application.properties"));
    Matcher property =
        Pattern.compile(
                "^app\\.sample-data\\.password=\\$\\{SAMPLE_USERS_PASSWORD:(.+)}$",
                Pattern.MULTILINE)
            .matcher(properties);

    assertThat(property.find()).isTrue();
    String readme = Files.readString(Path.of("../README.md"));
    assertThat(readme).contains("`" + property.group(1) + "`");
  }

  @Test
  void encodesTheConfiguredPasswordOnceAndStoresOnlyTheEncodedValue() {
    DataLoader other =
        new DataLoader(
            userRepository,
            tripRepository,
            activityRepository,
            imageRepository,
            passwordEncoder,
            ADMIN,
            "Other-2026");
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(userRepository.save(savedUser.capture()))
        .thenAnswer(invocation -> withId(invocation.getArgument(0), 5L));
    when(tripRepository.findByCreatorIdAndNameIn(eq(5L), anyCollection())).thenReturn(List.of());

    other.run();

    verify(passwordEncoder, times(1)).encode("Other-2026");
    assertThat(savedUser.getAllValues())
        .extracting(User::getPassword)
        .containsOnly("encoded-password");
  }

  @Test
  void createsTheThreeSampleUsersAsActiveRegularUsersInAnEmptyDatabase() {
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(userRepository.save(savedUser.capture()))
        .thenAnswer(invocation -> withId(invocation.getArgument(0), 5L));
    when(tripRepository.findByCreatorIdAndNameIn(eq(5L), anyCollection())).thenReturn(List.of());

    dataLoader.run();

    assertThat(savedUser.getAllValues())
        .extracting(User::getEmail)
        .containsExactly("ana@tripmate.com", "carlos@tripmate.com", "laura@tripmate.com");
    assertThat(savedUser.getAllValues())
        .allSatisfy(
            user -> {
              assertThat(user.getRole()).isEqualTo(Role.USER);
              assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
              assertThat(user.getPassword()).isEqualTo("encoded-password");
            });
  }

  @Test
  void assignsEverySampleTripToItsUser() {
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(userRepository.save(savedUser.capture()))
        .thenAnswer(
            invocation -> {
              User user = invocation.getArgument(0);
              long id = user.getEmail().startsWith("ana") ? 1L : 2L;
              return withId(user, id);
            });
    when(tripRepository.findByCreatorIdAndNameIn(anyLong(), anyCollection())).thenReturn(List.of());

    dataLoader.run();

    verify(tripRepository, times(3)).saveAll(savedTrips.capture());
    List<Trip> anas = tripsOf(savedTrips.getAllValues().get(0));
    assertThat(anas).hasSize(12);
    assertThat(anas).extracting(trip -> trip.getCreator().getId()).containsOnly(1L);
    assertThat(anas).extracting(Trip::getDestination).contains("Lisbon", "Rome", "Tokyo");
    assertThat(tripsOf(savedTrips.getAllValues().get(1))).hasSize(3);
    assertThat(tripsOf(savedTrips.getAllValues().get(2))).hasSize(2);
    assertThat(anas)
        .allSatisfy(trip -> assertThat(trip.getEndDate()).isAfterOrEqualTo(trip.getStartDate()));
  }

  @Test
  void onlyAddsWhatIsMissingAndKeepsExistingUsers() {
    User ana = UserTestData.user(1L, "ana@tripmate.com", Role.USER, UserStatus.ACTIVE);
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(userRepository.findByEmail("ana@tripmate.com")).thenReturn(Optional.of(ana));
    when(userRepository.save(savedUser.capture()))
        .thenAnswer(invocation -> withId(invocation.getArgument(0), 2L));
    when(tripRepository.findByCreatorIdAndNameIn(eq(1L), anyCollection()))
        .thenReturn(
            List.of(
                new Trip(
                    ana,
                    "Lisbon getaway",
                    "Lisboa",
                    LocalDate.of(2027, 5, 1),
                    LocalDate.of(2027, 5, 5))));
    when(tripRepository.findByCreatorIdAndNameIn(eq(2L), anyCollection())).thenReturn(List.of());

    dataLoader.run();

    assertThat(savedUser.getAllValues())
        .extracting(User::getEmail)
        .doesNotContain("ana@tripmate.com");
    verify(tripRepository, times(3)).saveAll(savedTrips.capture());
    List<Trip> anas = tripsOf(savedTrips.getAllValues().get(0));
    assertThat(anas).hasSize(11);
    assertThat(anas).extracting(Trip::getName).doesNotContain("Lisbon getaway");
  }

  @Test
  void aSecondRunWithEverythingLoadedAddsNothing() {
    when(userRepository.findByEmail(anyString()))
        .thenAnswer(
            invocation ->
                Optional.of(
                    UserTestData.user(
                        1L, invocation.getArgument(0), Role.USER, UserStatus.ACTIVE)));
    when(tripRepository.findByCreatorIdAndNameIn(eq(1L), anyCollection()))
        .thenAnswer(
            invocation -> {
              Iterable<String> names = invocation.getArgument(1);
              User creator = UserTestData.user(1L, "x@example.com", Role.USER, UserStatus.ACTIVE);
              List<Trip> existing = new java.util.ArrayList<>();
              for (String name : names) {
                Trip trip =
                    new Trip(
                        creator, name, "City", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 2));
                trip.setCoverImage(new Image(new byte[] {1}, "image/png"));
                existing.add(trip);
              }
              return existing;
            });

    when(activityRepository.existsByTripId(any())).thenReturn(true);

    dataLoader.run();

    verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    verify(tripRepository, times(4)).saveAll(savedTrips.capture());
    assertThat(savedTrips.getAllValues()).allSatisfy(trips -> assertThat(trips).isEmpty());
    verify(tripRepository, never()).save(any(Trip.class));
    verify(imageRepository, never()).save(any());
    verify(activityRepository, never())
        .saveAll(org.mockito.ArgumentMatchers.<Iterable<Activity>>any());
  }

  @Test
  @Tag("B1")
  void withoutAnAdministratorAccountTheExampleTripsAreSkipped() {
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(userRepository.save(savedUser.capture()))
        .thenAnswer(invocation -> withId(invocation.getArgument(0), 5L));
    when(tripRepository.findByCreatorIdAndNameIn(eq(5L), anyCollection())).thenReturn(List.of());

    dataLoader.run();

    verify(userRepository).findByEmail("admin@tripmate.com");
    verify(tripRepository, times(3)).saveAll(savedTrips.capture());
    assertThat(savedTrips.getAllValues().stream().flatMap(trips -> tripsOf(trips).stream()))
        .extracting(Trip::getName)
        .doesNotContainAnyElementsOf(
            DataLoader.SAMPLE_EXAMPLES.stream().map(DataLoader.SampleTrip::name).toList());
  }

  /** Every sample user already exists (id 1) and has none of its trips yet; saving gives ids. */
  private void emptyDatabaseWithSampleUsers() {
    when(userRepository.findByEmail(anyString()))
        .thenAnswer(
            invocation ->
                Optional.of(
                    UserTestData.user(
                        1L, invocation.getArgument(0), Role.USER, UserStatus.ACTIVE)));
    when(tripRepository.findByCreatorIdAndNameIn(anyLong(), anyCollection())).thenReturn(List.of());
    when(tripRepository.saveAll(org.mockito.ArgumentMatchers.<Iterable<Trip>>any()))
        .thenAnswer(
            invocation -> {
              List<Trip> trips = tripsOf(invocation.getArgument(0));
              long id = 100;
              for (Trip trip : trips) {
                ReflectionTestUtils.setField(trip, "id", id++);
              }
              return trips;
            });
    lenient()
        .when(imageRepository.save(any(Image.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void loadsTheItineraryOfTheSampleTripsThatHaveOneInsideTheirDates() {
    emptyDatabaseWithSampleUsers();
    when(activityRepository.existsByTripId(anyLong())).thenReturn(false);

    dataLoader.run();

    ArgumentCaptor<Iterable<Activity>> saved = ArgumentCaptor.captor();
    verify(activityRepository, times(8)).saveAll(saved.capture());
    List<Activity> all = new java.util.ArrayList<>();
    saved.getAllValues().forEach(batch -> batch.forEach(all::add));
    assertThat(all).hasSize(41);
    assertThat(all)
        .allSatisfy(
            activity -> {
              assertThat(activity.getDate())
                  .isBetween(activity.getTrip().getStartDate(), activity.getTrip().getEndDate());
              assertThat(activity.getAuthor()).isSameAs(activity.getTrip().getCreator());
              assertThat(activity.getName()).isNotBlank();
            });
    assertThat(all).filteredOn(a -> a.getTrip().getName().equals("Lisbon getaway")).hasSize(7);
    assertThat(all).filteredOn(a -> a.getTrip().getName().equals("Weekend in Porto")).hasSize(4);
    assertThat(all)
        .filteredOn(a -> a.getTrip().getName().equals("Weekend in Porto"))
        .extracting(Activity::getDate)
        .doesNotContain(LocalDate.of(2027, 3, 14));
  }

  @Test
  void someSampleActivitiesHaveAnImageAndOthersDoNot() {
    emptyDatabaseWithSampleUsers();
    when(activityRepository.existsByTripId(anyLong())).thenReturn(false);

    dataLoader.run();

    // Four activity pictures and the four covers of the example trips.
    ArgumentCaptor<Image> images = ArgumentCaptor.forClass(Image.class);
    verify(imageRepository, times(8)).save(images.capture());
    assertThat(images.getAllValues())
        .allSatisfy(
            image -> {
              assertThat(image.getContentType()).isEqualTo("image/png");
              assertThat(image.getData())
                  .startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47);
            });
  }

  @Test
  @Tag("B1")
  void theExampleTripsAreTheAdministratorsWithBudgetDescriptionAndCover() {
    emptyDatabaseWithSampleUsers();
    when(activityRepository.existsByTripId(anyLong())).thenReturn(false);

    dataLoader.run();

    verify(userRepository).findByEmail("admin@tripmate.com");
    verify(tripRepository, times(4)).saveAll(savedTrips.capture());
    List<Trip> examples = tripsOf(savedTrips.getAllValues().get(3));
    assertThat(examples)
        .extracting(Trip::getName)
        .containsExactly(
            "Rome in five days", "Paris in four days", "Maldives escape", "Tokyo highlights");
    assertThat(examples)
        .allSatisfy(
            trip -> {
              assertThat(trip.getBudget()).isNotNull();
              assertThat(trip.getDescription()).isNotBlank();
            });
    ArgumentCaptor<Trip> withCover = ArgumentCaptor.forClass(Trip.class);
    verify(tripRepository, times(4)).save(withCover.capture());
    assertThat(withCover.getAllValues())
        .extracting(Trip::getName)
        .containsExactlyInAnyOrderElementsOf(examples.stream().map(Trip::getName).toList());
    assertThat(withCover.getAllValues())
        .allSatisfy(trip -> assertThat(trip.getCoverImage()).isNotNull());
  }

  @Test
  void aTripThatAlreadyHasActivitiesIsLeftAlone() {
    emptyDatabaseWithSampleUsers();
    when(activityRepository.existsByTripId(anyLong())).thenReturn(true);

    dataLoader.run();

    verify(activityRepository, never())
        .saveAll(org.mockito.ArgumentMatchers.<Iterable<Activity>>any());
    // Only the covers of the example trips: no activity picture is created.
    verify(imageRepository, times(4)).save(any());
  }

  @Test
  @Tag("B1")
  void everyExampleTripHasItsPhotoAndActivitiesOnItsFirstThreeDays() {
    assertThat(DataLoader.SAMPLE_EXAMPLES)
        .hasSize(4)
        .allSatisfy(
            trip -> {
              assertThat(Path.of("src/main/resources/sample_images", trip.cover())).exists();
              assertThat(trip.budget()).isNotNull();
              assertThat(DataLoader.SAMPLE_ACTIVITIES.get(trip.name()))
                  .extracting(DataLoader.SampleActivity::day)
                  .contains(0, 1, 2);
            });
  }

  @Test
  void everySampleItineraryBelongsToASampleTripAndFitsItsDays() {
    List<DataLoader.SampleTrip> trips =
        java.util.stream.Stream.concat(
                DataLoader.SAMPLE_USERS.stream().flatMap(user -> user.trips().stream()),
                DataLoader.SAMPLE_EXAMPLES.stream())
            .toList();

    DataLoader.SAMPLE_ACTIVITIES.forEach(
        (tripName, activities) -> {
          DataLoader.SampleTrip trip =
              trips.stream().filter(t -> t.name().equals(tripName)).findFirst().orElseThrow();
          long days = trip.endDate().toEpochDay() - trip.startDate().toEpochDay();
          assertThat(activities)
              .allSatisfy(
                  activity -> {
                    assertThat(activity.day()).isBetween(0, (int) days);
                    if (activity.end() != null) {
                      assertThat(activity.end()).isAfter(activity.start());
                    }
                  });
        });
  }

  @Test
  void theGeneratedPictureIsAValidPng() {
    byte[] png = DataLoader.solidPng(0x2E6F9E);

    assertThat(png).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47);
  }

  private static List<Trip> tripsOf(Iterable<Trip> trips) {
    List<Trip> list = new java.util.ArrayList<>();
    trips.forEach(list::add);
    return list;
  }
}
