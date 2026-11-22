package com.ruben.backend.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.activity.entity.Activity;
import com.ruben.backend.activity.repository.ActivityRepository;
import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.BadRequestException;
import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.common.security.TripMateUserDetails;
import com.ruben.backend.common.security.TripMateUserDetailsService;
import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.image.dto.ImageResult;
import com.ruben.backend.image.entity.Image;
import com.ruben.backend.image.repository.ImageRepository;
import com.ruben.backend.participation.service.ParticipationService;
import com.ruben.backend.trip.dto.AdminTripDTO;
import com.ruben.backend.trip.dto.TripDTO;
import com.ruben.backend.trip.dto.TripRequest;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.dto.RegisterRequest;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AdminAccountInitializer;
import com.ruben.backend.user.service.UserService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@Tag("B4")
@Tag("B8")
@Tag("I9")
@Tag("I10")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TripServiceIntegrationTest {

  @Autowired private TripService tripService;
  @Autowired private PublicTripService publicTripService;
  @Autowired private UserService userService;
  @Autowired private ParticipationService participationService;
  @Autowired private TripRepository tripRepository;
  @Autowired private ImageRepository imageRepository;
  @Autowired private ActivityRepository activityRepository;
  @Autowired private TripMateUserDetailsService userDetailsService;
  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private UserRepository userRepository;
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

  /** Makes the user the one of the current request, as the JWT filter would. */
  private void logInAs(UserDTO user) {
    TripMateUserDetails details = userDetailsService.loadUserById(user.id());
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                details, null, details.getAuthorities()));
  }

  private static TripRequest request(String name, int dayOffset) {
    LocalDate start = LocalDate.of(2027, 1, 1).plusDays(dayOffset);
    return new TripRequest(name, "City", start, start.plusDays(2), null, null);
  }

  private void createTrips(int count, String prefix) {
    for (int i = 1; i <= count; i++) {
      tripService.create(request(prefix + " " + i, i));
    }
  }

  private static MockMultipartFile png() {
    return new MockMultipartFile("file", "a.png", "image/png", ImageTestData.png(100));
  }

  @Test
  @Tag("I7")
  void publishingPersistsTheFlagAndTheDateAndUnpublishingClearsThem() {
    TripDTO created = tripService.create(request("Rome", 1));
    assertThat(created.published()).isFalse();

    TripDTO published = tripService.changePublication(created.id(), true);

    assertThat(published.published()).isTrue();
    assertThat(published.publishedAt()).isNotNull();
    Trip stored = tripRepository.findById(created.id()).orElseThrow();
    assertThat(stored.isPublished()).isTrue();
    assertThat(stored.getPublishedAt()).isNotNull();

    TripDTO again = tripService.changePublication(created.id(), true);
    assertThat(again.publishedAt()).isEqualTo(stored.getPublishedAt());

    TripDTO unpublished = tripService.changePublication(created.id(), false);
    assertThat(unpublished.published()).isFalse();
    Trip unpublishedStored = tripRepository.findById(created.id()).orElseThrow();
    assertThat(unpublishedStored.isPublished()).isFalse();
    assertThat(unpublishedStored.getPublishedAt()).isNull();
  }

  @Test
  @Tag("I7")
  void onlyTheCreatorMayPublishNotAnotherUserNorTheAdministrator() {
    TripDTO created = tripService.create(request("Rome", 1));

    logInAs(bob);
    assertThatThrownBy(() -> tripService.changePublication(created.id(), true))
        .isInstanceOf(ForbiddenException.class);

    logInAs(UserDTO.from(userRepository.findByEmail("admin@test.tripmate.com").orElseThrow()));
    assertThatThrownBy(() -> tripService.changePublication(created.id(), true))
        .isInstanceOf(ForbiddenException.class);
    assertThat(tripRepository.findById(created.id()).orElseThrow().isPublished()).isFalse();
  }

  @Test
  @Tag("I10")
  void theAdministratorUnpublishesAnotherUsersTripAndItsDataIsKept() {
    TripDTO created = tripService.create(request("Rome", 1));
    tripService.changePublication(created.id(), true);
    activityRepository.save(
        new Activity(
            tripRepository.getReferenceById(created.id()),
            userRepository.getReferenceById(ana.id()),
            "Colosseum",
            LocalDate.of(2027, 1, 3)));
    assertThat(publicTripService.findPage(null, PageRequest.of(0, 10)).content()).hasSize(1);

    logInAs(UserDTO.from(userRepository.findByEmail("admin@test.tripmate.com").orElseThrow()));
    TripDTO unpublished = tripService.changePublication(created.id(), false);

    assertThat(unpublished.published()).isFalse();
    assertThat(unpublished.creatorId()).isEqualTo(ana.id());
    assertThat(publicTripService.findPage(null, PageRequest.of(0, 10)).content()).isEmpty();
    Trip stored = tripRepository.findById(created.id()).orElseThrow();
    assertThat(stored.isPublished()).isFalse();
    assertThat(stored.getPublishedAt()).isNull();
    assertThat(activityRepository.count()).isEqualTo(1);
  }

  @Test
  @Tag("I10")
  void anotherRegularUserMayNotUnpublishAndTheTripStaysPublic() {
    TripDTO created = tripService.create(request("Rome", 1));
    tripService.changePublication(created.id(), true);

    logInAs(bob);
    assertThatThrownBy(() -> tripService.changePublication(created.id(), false))
        .isInstanceOf(ForbiddenException.class);

    assertThat(tripRepository.findById(created.id()).orElseThrow().isPublished()).isTrue();
  }

  @Test
  @Tag("I10")
  void theAdministratorUnpublishingAMissingTripIsNotFound() {
    logInAs(UserDTO.from(userRepository.findByEmail("admin@test.tripmate.com").orElseThrow()));

    assertThatThrownBy(() -> tripService.changePublication(999_999L, false))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @Tag("I7")
  void publishingAMissingTripIsNotFound() {
    assertThatThrownBy(() -> tripService.changePublication(999_999L, true))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void createPersistsTheTripForTheCurrentUserWithBudgetAndDescription() {
    TripDTO created =
        tripService.create(
            new TripRequest(
                "  Lisbon ",
                "Lisbon",
                LocalDate.of(2027, 5, 1),
                LocalDate.of(2027, 5, 5),
                new BigDecimal("1200.5"),
                "Pastéis"));

    Trip stored = tripRepository.findById(created.id()).orElseThrow();
    assertThat(stored.getCreator().getId()).isEqualTo(ana.id());
    assertThat(stored.getName()).isEqualTo("Lisbon");
    assertThat(stored.getBudget()).isEqualByComparingTo("1200.50");
    assertThat(stored.getDescription()).isEqualTo("Pastéis");
    assertThat(stored.getCreatedAt()).isNotNull();
    assertThat(stored.getCoverImage()).isNull();
  }

  @Test
  void findPageReturnsOnlyTheCurrentUsersTripsOrderedByStartDate() {
    createTrips(12, "Ana trip");
    logInAs(bob);
    createTrips(3, "Bob trip");
    logInAs(ana);

    PageDTO<TripDTO> first = tripService.findPage(PageRequest.of(0, 10, Sort.by("startDate")));
    PageDTO<TripDTO> second = tripService.findPage(PageRequest.of(1, 10, Sort.by("startDate")));

    assertThat(first.content()).hasSize(10);
    assertThat(first.content().get(0).name()).isEqualTo("Ana trip 1");
    assertThat(first.content().get(9).name()).isEqualTo("Ana trip 10");
    assertThat(first.totalElements()).isEqualTo(12);
    assertThat(first.last()).isFalse();
    assertThat(second.content())
        .extracting(TripDTO::name)
        .containsExactly("Ana trip 11", "Ana trip 12");
    assertThat(second.last()).isTrue();
    assertThat(first.content()).extracting(TripDTO::creatorId).containsOnly(ana.id());
  }

  private void join(UserDTO user, TripDTO trip) {
    participationService.add(
        tripRepository.findById(trip.id()).orElseThrow(),
        userRepository.getReferenceById(user.id()));
  }

  @Test
  @Tag("I2")
  void findPageListsTheTripsTheUserCreatedAndTheOnesTheyJoinedAndNoOtherTrips() {
    createTrips(2, "Ana trip");
    logInAs(bob);
    TripDTO joined = tripService.create(request("Bob joined", 5));
    tripService.create(request("Bob private", 6));
    TripDTO alsoJoined = tripService.create(request("Bob joined too", 7));
    join(ana, joined);
    join(ana, alsoJoined);
    logInAs(ana);

    PageDTO<TripDTO> first = tripService.findPage(PageRequest.of(0, 3, Sort.by("startDate")));
    PageDTO<TripDTO> second = tripService.findPage(PageRequest.of(1, 3, Sort.by("startDate")));

    assertThat(first.content())
        .extracting(TripDTO::name)
        .containsExactly("Ana trip 1", "Ana trip 2", "Bob joined");
    assertThat(first.totalElements()).isEqualTo(4);
    assertThat(second.content()).extracting(TripDTO::name).containsExactly("Bob joined too");
    assertThat(second.last()).isTrue();
  }

  @Test
  @Tag("I2")
  void aParticipantReadsTheTripButCannotChangeOrDeleteIt() {
    logInAs(bob);
    TripDTO trip = tripService.create(request("Bob trip", 1));
    join(ana, trip);
    logInAs(ana);

    assertThat(tripService.findById(trip.id()).name()).isEqualTo("Bob trip");
    assertThatThrownBy(() -> tripService.update(trip.id(), request("Mine now", 1)))
        .isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> tripService.delete(trip.id())).isInstanceOf(ForbiddenException.class);
  }

  @Test
  void findPageSortsDescendingInTheQuery() {
    createTrips(3, "Ana trip");

    PageDTO<TripDTO> page =
        tripService.findPage(PageRequest.of(0, 10, Sort.by("name").descending()));

    assertThat(page.content())
        .extracting(TripDTO::name)
        .containsExactly("Ana trip 3", "Ana trip 2", "Ana trip 1");
  }

  @Test
  void findPageRefusesToSortByTheCreatorsPassword() {
    assertThatThrownBy(
            () -> tripService.findPage(PageRequest.of(0, 10, Sort.by("creator.password"))))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void updateReplacesTheFieldsAndKeepsTheCover() {
    TripDTO created = tripService.create(request("Old", 1));
    tripService.replaceCover(created.id(), png());

    TripDTO updated =
        tripService.update(
            created.id(),
            new TripRequest(
                "New",
                "Oslo",
                LocalDate.of(2028, 2, 1),
                LocalDate.of(2028, 2, 3),
                new BigDecimal("10"),
                "Cold"));

    assertThat(updated.name()).isEqualTo("New");
    assertThat(updated.destination()).isEqualTo("Oslo");
    assertThat(updated.budget()).isEqualByComparingTo("10.00");
    assertThat(updated.coverImageId()).isNotNull();
    assertThat(tripService.findById(created.id()).description()).isEqualTo("Cold");
  }

  @Test
  void anotherUsersTripCannotBeReadChangedOrDeleted() {
    TripDTO created = tripService.create(request("Ana's", 1));
    logInAs(bob);

    assertThatThrownBy(() -> tripService.findById(created.id()))
        .isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> tripService.update(created.id(), request("Mine now", 1)))
        .isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> tripService.delete(created.id()))
        .isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> tripService.replaceCover(created.id(), png()))
        .isInstanceOf(ForbiddenException.class);
    assertThat(tripRepository.findById(created.id()).orElseThrow().getName()).isEqualTo("Ana's");
    assertThat(imageRepository.count()).isZero();
  }

  @Test
  void aMissingTripIsNotFound() {
    assertThatThrownBy(() -> tripService.findById(999_999L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void deleteRemovesTheTripAndItsCoverImage() {
    TripDTO created = tripService.create(request("With cover", 1));
    tripService.replaceCover(created.id(), png());
    assertThat(imageRepository.count()).isEqualTo(1);

    tripService.delete(created.id());

    assertThat(tripRepository.existsById(created.id())).isFalse();
    assertThat(imageRepository.count()).isZero();
  }

  @Test
  void deleteOfATripWithoutCoverLeavesOtherTripsAndImagesAlone() {
    TripDTO kept = tripService.create(request("Kept", 1));
    tripService.replaceCover(kept.id(), png());
    TripDTO removed = tripService.create(request("Removed", 2));

    tripService.delete(removed.id());

    assertThat(tripRepository.existsById(kept.id())).isTrue();
    assertThat(imageRepository.count()).isEqualTo(1);
  }

  @Test
  void findAllPageSearchesEveryUsersTripsIgnoringCaseAndPages() {
    tripService.create(request("Lisbon", 1));
    tripService.create(
        new TripRequest(
            "Rome", "Sicily", LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 3), null, null));
    logInAs(bob);
    tripService.create(request("Oslo", 2));
    PageRequest newestFirst = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt", "id"));

    PageDTO<AdminTripDTO> all = tripService.findAllPage(null, newestFirst);
    assertThat(all.content()).extracting(AdminTripDTO::name).containsExactly("Oslo", "Rome");
    assertThat(all.totalElements()).isEqualTo(3);
    assertThat(all.last()).isFalse();
    assertThat(all.content().get(0).creatorEmail()).isEqualTo("bob@example.com");

    assertThat(tripService.findAllPage(" LISB ", newestFirst).content())
        .extracting(AdminTripDTO::name)
        .containsExactly("Lisbon");
    assertThat(tripService.findAllPage("sicily", newestFirst).content())
        .extracting(AdminTripDTO::name)
        .containsExactly("Rome");
    assertThat(tripService.findAllPage("ANA@EXAMPLE", newestFirst).content())
        .extracting(AdminTripDTO::name)
        .containsExactlyInAnyOrder("Lisbon", "Rome");
    assertThat(tripService.findAllPage("zzz", newestFirst).content()).isEmpty();
  }

  @Test
  void theAdministratorReadsAndDeletesAnotherUsersTripButCannotChangeIt() {
    TripDTO created = tripService.create(request("Ana's", 1));
    tripService.replaceCover(created.id(), png());
    logInAs(UserDTO.from(userRepository.findByEmail("admin@test.tripmate.com").orElseThrow()));

    assertThat(tripService.findById(created.id()).name()).isEqualTo("Ana's");
    assertThatThrownBy(() -> tripService.update(created.id(), request("Mine now", 1)))
        .isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> tripService.replaceCover(created.id(), png()))
        .isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> tripService.deleteCover(created.id()))
        .isInstanceOf(ForbiddenException.class);

    tripService.delete(created.id());

    assertThat(tripRepository.existsById(created.id())).isFalse();
    assertThat(imageRepository.count()).isZero();
  }

  @Test
  void replacingTheCoverDeletesThePreviousImageRow() {
    TripDTO created = tripService.create(request("Cover", 1));
    ImageResult first = tripService.replaceCover(created.id(), png());
    ImageResult second = tripService.replaceCover(created.id(), png());

    assertThat(first.created()).isTrue();
    assertThat(second.created()).isFalse();
    assertThat(imageRepository.existsById(first.image().id())).isFalse();
    assertThat(imageRepository.existsById(second.image().id())).isTrue();
    assertThat(imageRepository.count()).isEqualTo(1);
    assertThat(tripService.findById(created.id()).coverImageId()).isEqualTo(second.image().id());
    assertThat(tripService.findCoverContent(created.id()).contentType()).isEqualTo("image/png");
  }

  @Test
  void deleteCoverClearsTheLinkAndTheImageRow() {
    TripDTO created = tripService.create(request("Cover", 1));
    tripService.replaceCover(created.id(), png());

    tripService.deleteCover(created.id());

    assertThat(tripService.findById(created.id()).coverImageId()).isNull();
    assertThat(imageRepository.count()).isZero();
    assertThatThrownBy(() -> tripService.findCoverContent(created.id()))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  // --- copy of a public trip (I9) ---

  /** Ana's published trip: a cover and two activities, one with a picture and times. */
  private Trip publishedTripOfAna() {
    Trip original =
        new Trip(
            userRepository.getReferenceById(ana.id()),
            "Rome trip",
            "Rome",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 17));
    original.setBudget(new BigDecimal("900.00"));
    original.setDescription("Ancient ruins");
    original.setCoverImage(imageRepository.save(new Image(ImageTestData.png(30), "image/png")));
    original.publish();
    original = tripRepository.save(original);
    Activity colosseum =
        new Activity(
            original,
            userRepository.getReferenceById(ana.id()),
            "Colosseum",
            LocalDate.of(2027, 7, 11));
    colosseum.setStartTime(LocalTime.of(10, 0));
    colosseum.setEndTime(LocalTime.of(12, 0));
    colosseum.setCost(new BigDecimal("16.50"));
    colosseum.setImage(imageRepository.save(new Image(ImageTestData.jpeg(40), "image/jpeg")));
    activityRepository.save(colosseum);
    activityRepository.save(
        new Activity(
            original,
            userRepository.getReferenceById(ana.id()),
            "Forum",
            LocalDate.of(2027, 7, 13)));
    return original;
  }

  private List<Activity> activitiesOf(Long tripId) {
    return activityRepository.findByTripIdOrderByDateAscStartTimeAscIdAsc(tripId);
  }

  @Test
  void copyPublicCreatesAnUnpublishedTripOfTheCallerLinkedToTheOriginal() {
    Trip original = publishedTripOfAna();
    logInAs(bob);

    TripDTO copy = tripService.copyPublic(original.getId(), null);

    assertThat(copy.id()).isNotEqualTo(original.getId());
    assertThat(copy.creatorId()).isEqualTo(bob.id());
    assertThat(copy.published()).isFalse();
    assertThat(copy.publishedAt()).isNull();
    assertThat(copy.copiedFromId()).isEqualTo(original.getId());
    assertThat(copy.name()).isEqualTo("Rome trip");
    assertThat(copy.destination()).isEqualTo("Rome");
    assertThat(copy.budget()).isEqualByComparingTo("900.00");
    assertThat(copy.description()).isEqualTo("Ancient ruins");
    assertThat(copy.startDate()).isEqualTo(LocalDate.of(2027, 7, 10));
    assertThat(copy.endDate()).isEqualTo(LocalDate.of(2027, 7, 17));
    assertThat(tripService.findPage(PageRequest.of(0, 10)).content())
        .extracting(TripDTO::id)
        .containsExactly(copy.id());
  }

  @Test
  void copyPublicCopiesTheActivitiesWithTheCallerAsAuthorAndKeepsTheDatesWithoutAStartDate() {
    Trip original = publishedTripOfAna();
    logInAs(bob);

    TripDTO copy = tripService.copyPublic(original.getId(), null);

    List<Activity> copied = activitiesOf(copy.id());
    assertThat(copied).extracting(Activity::getName).containsExactly("Colosseum", "Forum");
    assertThat(copied)
        .extracting(Activity::getDate)
        .containsExactly(LocalDate.of(2027, 7, 11), LocalDate.of(2027, 7, 13));
    assertThat(copied.getFirst().getAuthor().getId()).isEqualTo(bob.id());
    assertThat(copied.getFirst().getStartTime()).isEqualTo(LocalTime.of(10, 0));
    assertThat(copied.getFirst().getEndTime()).isEqualTo(LocalTime.of(12, 0));
    assertThat(copied.getFirst().getCost()).isEqualByComparingTo("16.50");
    assertThat(activitiesOf(original.getId())).hasSize(2);
  }

  @Test
  void copyPublicWithAStartDateMovesTheTripAndEveryActivityByTheSameDays() {
    Trip original = publishedTripOfAna();
    logInAs(bob);

    TripDTO copy = tripService.copyPublic(original.getId(), LocalDate.of(2027, 9, 1));

    assertThat(copy.startDate()).isEqualTo(LocalDate.of(2027, 9, 1));
    assertThat(copy.endDate()).isEqualTo(LocalDate.of(2027, 9, 8));
    assertThat(activitiesOf(copy.id()))
        .extracting(Activity::getDate)
        .containsExactly(LocalDate.of(2027, 9, 2), LocalDate.of(2027, 9, 4));
    assertThat(activitiesOf(original.getId()))
        .extracting(Activity::getDate)
        .containsExactly(LocalDate.of(2027, 7, 11), LocalDate.of(2027, 7, 13));
  }

  @Test
  void copyPublicDuplicatesTheCoverAndThePicturesAsNewRowsWithTheSameContent() {
    Trip original = publishedTripOfAna();
    UUID originalCover = original.getCoverImage().getId();
    UUID originalPicture = activitiesOf(original.getId()).getFirst().getImage().getId();
    logInAs(bob);

    TripDTO copy = tripService.copyPublic(original.getId(), null);

    UUID copiedPicture = activitiesOf(copy.id()).getFirst().getImage().getId();
    assertThat(copy.coverImageId()).isNotNull().isNotEqualTo(originalCover);
    assertThat(copiedPicture).isNotEqualTo(originalPicture);
    assertThat(imageRepository.count()).isEqualTo(4);
    Image cover = imageRepository.findById(copy.coverImageId()).orElseThrow();
    assertThat(cover.getData()).isEqualTo(ImageTestData.png(30));
    assertThat(cover.getContentType()).isEqualTo("image/png");
    Image picture = imageRepository.findById(copiedPicture).orElseThrow();
    assertThat(picture.getData()).isEqualTo(ImageTestData.jpeg(40));
    assertThat(picture.getContentType()).isEqualTo("image/jpeg");
  }

  @Test
  void copyPublicOfAnUnpublishedOrUnknownTripIsNotFoundAndCreatesNothing() {
    Trip original = publishedTripOfAna();
    original.unpublish();
    tripRepository.save(original);
    logInAs(bob);

    assertThatThrownBy(() -> tripService.copyPublic(original.getId(), null))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> tripService.copyPublic(999_999L, null))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThat(tripRepository.count()).isEqualTo(1);
    assertThat(imageRepository.count()).isEqualTo(2);
  }

  @Test
  void theCopyIsIndependentOfTheOriginalWhenTheOriginalIsEdited() {
    Trip original = publishedTripOfAna();
    logInAs(bob);
    TripDTO copy = tripService.copyPublic(original.getId(), null);

    logInAs(ana);
    tripService.update(
        original.getId(),
        new TripRequest(
            "Renamed", "Paris", LocalDate.of(2028, 1, 1), LocalDate.of(2028, 1, 3), null, null));
    tripService.changePublication(original.getId(), false);

    logInAs(bob);
    TripDTO unchanged = tripService.findById(copy.id());
    assertThat(unchanged.name()).isEqualTo("Rome trip");
    assertThat(unchanged.destination()).isEqualTo("Rome");
    assertThat(unchanged.startDate()).isEqualTo(LocalDate.of(2027, 7, 10));
    assertThat(unchanged.copiedFromId()).isEqualTo(original.getId());
  }

  @Test
  void deletingTheOriginalKeepsTheCopyWorkingAndClearsItsLink() {
    Trip original = publishedTripOfAna();
    logInAs(bob);
    TripDTO copy = tripService.copyPublic(original.getId(), null);

    logInAs(ana);
    tripService.delete(original.getId());

    logInAs(bob);
    TripDTO survivor = tripService.findById(copy.id());
    assertThat(survivor.copiedFromId()).isNull();
    assertThat(survivor.name()).isEqualTo("Rome trip");
    assertThat(activitiesOf(copy.id())).hasSize(2);
    assertThat(imageRepository.count()).isEqualTo(2);
    assertThat(imageRepository.findById(survivor.coverImageId())).isPresent();
    UUID picture = activitiesOf(copy.id()).getFirst().getImage().getId();
    assertThat(imageRepository.findById(picture)).isPresent();
  }

  @Test
  void deletingTheCopyLeavesTheOriginalWithItsActivitiesAndImages() {
    Trip original = publishedTripOfAna();
    logInAs(bob);
    TripDTO copy = tripService.copyPublic(original.getId(), null);

    tripService.delete(copy.id());

    assertThat(tripRepository.findById(original.getId())).isPresent();
    assertThat(activitiesOf(original.getId())).hasSize(2);
    assertThat(imageRepository.count()).isEqualTo(2);
  }
}
