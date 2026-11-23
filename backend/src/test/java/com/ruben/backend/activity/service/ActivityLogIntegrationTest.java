package com.ruben.backend.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.activity.dto.ActivityDTO;
import com.ruben.backend.activity.dto.ActivityLogEntryDTO;
import com.ruben.backend.activity.dto.ActivityRequest;
import com.ruben.backend.activity.entity.ActivityLogAction;
import com.ruben.backend.activity.repository.ActivityLogEntryRepository;
import com.ruben.backend.activity.repository.ActivityRepository;
import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.BadRequestException;
import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.common.security.TripMateUserDetails;
import com.ruben.backend.common.security.TripMateUserDetailsService;
import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.invitation.dto.InvitationRequest;
import com.ruben.backend.invitation.entity.InvitationStatus;
import com.ruben.backend.invitation.service.InvitationService;
import com.ruben.backend.participation.service.ParticipantService;
import com.ruben.backend.trip.dto.TripDTO;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@Tag("I4")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ActivityLogIntegrationTest {

  private static final LocalDate START = LocalDate.of(2027, 7, 10);
  private static final LocalDate END = LocalDate.of(2027, 7, 14);

  @Autowired private ActivityService activityService;
  @Autowired private ActivityLogService activityLogService;
  @Autowired private TripService tripService;
  @Autowired private UserService userService;
  @Autowired private InvitationService invitationService;
  @Autowired private ParticipantService participantService;
  @Autowired private ActivityRepository activityRepository;
  @Autowired private ActivityLogEntryRepository entryRepository;
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
    tripId = newTrip().id();
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

  private TripDTO newTrip() {
    return tripService.create(new TripRequest("Rome", "Rome", START, END, null, null));
  }

  /** Ana invites the user, who accepts; the user stays logged in. */
  private void join(UserDTO user) {
    logInAs(ana);
    long id = invitationService.create(tripId, new InvitationRequest(user.email())).id();
    logInAs(user);
    invitationService.respond(id, InvitationStatus.ACCEPTED);
  }

  private static ActivityRequest request(String name, BigDecimal cost) {
    return new ActivityRequest(name, START.plusDays(1), null, null, null, null, cost, null);
  }

  private PageDTO<ActivityLogEntryDTO> log(long trip) {
    return activityLogService.findPage(trip, PageRequest.of(0, 50, newestFirst()));
  }

  private static Sort newestFirst() {
    return Sort.by(Sort.Direction.DESC, "createdAt", "id");
  }

  @Test
  void everyChangeWritesExactlyOneLineWithItsActionUserAndChangedFields() {
    ActivityDTO museum = activityService.create(tripId, request("Museum", null));
    join(bob);

    activityService.update(tripId, museum.id(), request("Louvre", new BigDecimal("20.00")));
    activityService.delete(tripId, museum.id());

    logInAs(ana);
    assertThat(log(tripId).content())
        .extracting(
            ActivityLogEntryDTO::action,
            ActivityLogEntryDTO::userName,
            ActivityLogEntryDTO::userId,
            ActivityLogEntryDTO::activityName,
            ActivityLogEntryDTO::changedFields)
        .containsExactly(
            tuple(ActivityLogAction.DELETED, "Bob", bob.id(), "Louvre", java.util.List.of()),
            tuple(
                ActivityLogAction.UPDATED,
                "Bob",
                bob.id(),
                "Louvre",
                java.util.List.of("name", "cost")),
            tuple(ActivityLogAction.CREATED, "Ana", ana.id(), "Museum", java.util.List.of()));
  }

  @Test
  void whenAnActivityIsDeletedItsLinesStayAndForgetItAndTheNamesAreKept() {
    ActivityDTO museum = activityService.create(tripId, request("Museum", null));
    activityService.update(tripId, museum.id(), request("Louvre", null));

    activityService.delete(tripId, museum.id());

    assertThat(activityRepository.count()).isZero();
    assertThat(log(tripId).content())
        .extracting(ActivityLogEntryDTO::activityId, ActivityLogEntryDTO::activityName)
        .containsExactly(tuple(null, "Louvre"), tuple(null, "Louvre"), tuple(null, "Museum"));
  }

  @Test
  void changingThePictureIsAnEditOfTheImageFieldAndRemovingNoneWritesNothing() {
    ActivityDTO museum = activityService.create(tripId, request("Museum", null));
    MockMultipartFile picture =
        new MockMultipartFile("file", "a.png", "image/png", ImageTestData.png(100));

    activityService.replaceImage(tripId, museum.id(), picture);
    activityService.deleteImage(tripId, museum.id());
    activityService.deleteImage(tripId, museum.id());

    assertThat(log(tripId).content())
        .extracting(ActivityLogEntryDTO::action, ActivityLogEntryDTO::changedFields)
        .containsExactly(
            tuple(ActivityLogAction.UPDATED, java.util.List.of("image")),
            tuple(ActivityLogAction.UPDATED, java.util.List.of("image")),
            tuple(ActivityLogAction.CREATED, java.util.List.of()));
  }

  @Test
  void aChangeThatFailsLeavesNoLine() {
    ActivityDTO museum = activityService.create(tripId, request("Museum", null));
    join(bob);
    long before = entryRepository.count();
    ActivityRequest outside =
        new ActivityRequest("Late", END.plusDays(1), null, null, null, null, null, null);
    MockMultipartFile empty = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);

    assertThatThrownBy(() -> activityService.create(tripId, outside))
        .isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> activityService.update(tripId, museum.id(), outside))
        .isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> activityService.update(tripId, 999_999L, request("Ghost", null)))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> activityService.delete(tripId, 999_999L))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> activityService.replaceImage(tripId, museum.id(), empty))
        .isInstanceOf(BadRequestException.class);
    logInAs(carla);
    assertThatThrownBy(() -> activityService.create(tripId, request("Mine", null)))
        .isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> activityService.delete(tripId, museum.id()))
        .isInstanceOf(ForbiddenException.class);

    assertThat(entryRepository.count()).isEqualTo(before);
    assertThat(activityRepository.count()).isEqualTo(1);
  }

  @Test
  void theLogIsPagedNewestFirstAndCanBeSortedOldestFirst() {
    activityService.create(tripId, request("First", null));
    activityService.create(tripId, request("Second", null));
    activityService.create(tripId, request("Third", null));
    PageRequest newest = PageRequest.of(0, 2, newestFirst());

    PageDTO<ActivityLogEntryDTO> first = activityLogService.findPage(tripId, newest);
    PageDTO<ActivityLogEntryDTO> second = activityLogService.findPage(tripId, newest.next());
    PageDTO<ActivityLogEntryDTO> oldest =
        activityLogService.findPage(
            tripId, PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt", "id")));

    assertThat(first.totalElements()).isEqualTo(3);
    assertThat(first.last()).isFalse();
    assertThat(first.content())
        .extracting(ActivityLogEntryDTO::activityName)
        .containsExactly("Third", "Second");
    assertThat(second.last()).isTrue();
    assertThat(second.content())
        .extracting(ActivityLogEntryDTO::activityName)
        .containsExactly("First");
    assertThat(oldest.content())
        .extracting(ActivityLogEntryDTO::activityName)
        .containsExactly("First", "Second", "Third");
    assertThat(oldest.content().getFirst().createdAt()).isNotNull();
  }

  @Test
  void theCreatorAndTheParticipantsReadTheLogButAStrangerDoesNot() {
    activityService.create(tripId, request("Museum", null));
    join(bob);

    assertThat(log(tripId).totalElements()).isEqualTo(1);
    logInAs(ana);
    assertThat(log(tripId).totalElements()).isEqualTo(1);
    logInAs(carla);
    assertThatThrownBy(() -> log(tripId)).isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> log(999_999L)).isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void deletingTheTripDeletesItsLogAndOnlyItsLog() {
    activityService.create(tripId, request("Museum", null));
    long otherTrip = newTrip().id();
    activityService.create(otherTrip, request("Park", null));

    tripService.delete(tripId);

    assertThat(entryRepository.count()).isEqualTo(1);
    assertThat(log(otherTrip).content())
        .extracting(ActivityLogEntryDTO::activityName)
        .containsExactly("Park");
  }

  @Test
  void aRemovedParticipantKeepsTheirActivitiesAndTheirLogLines() {
    join(bob);
    ActivityDTO walk = activityService.create(tripId, request("Bob's walk", null));
    logInAs(ana);

    participantService.remove(tripId, bob.id());

    assertThat(activityService.findById(tripId, walk.id()).authorName()).isEqualTo("Bob");
    assertThat(log(tripId).content())
        .extracting(ActivityLogEntryDTO::userName, ActivityLogEntryDTO::action)
        .containsExactly(tuple("Bob", ActivityLogAction.CREATED));
    activityService.update(tripId, walk.id(), request("Our walk", null));
    assertThat(log(tripId).totalElements()).isEqualTo(2);
  }

  @Test
  void theActivitiesOfAPageCarryTheNameOfWhoAddedThem() {
    join(bob);
    activityService.create(tripId, request("Bob's walk", null));
    logInAs(ana);
    activityService.create(tripId, request("Ana's museum", null));

    assertThat(
            activityService.findPage(tripId, null, PageRequest.of(0, 10, Sort.by("id"))).content())
        .extracting(ActivityDTO::name, ActivityDTO::authorName)
        .containsExactly(tuple("Bob's walk", "Bob"), tuple("Ana's museum", "Ana"));
  }
}
