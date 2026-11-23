package com.ruben.backend.participation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.ConflictException;
import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.common.security.TripMateUserDetails;
import com.ruben.backend.common.security.TripMateUserDetailsService;
import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.invitation.dto.InvitationRequest;
import com.ruben.backend.invitation.entity.Invitation;
import com.ruben.backend.invitation.entity.InvitationStatus;
import com.ruben.backend.invitation.repository.InvitationRepository;
import com.ruben.backend.invitation.service.InvitationService;
import com.ruben.backend.participation.dto.ParticipantDTO;
import com.ruben.backend.participation.repository.ParticipationRepository;
import com.ruben.backend.trip.dto.TripDTO;
import com.ruben.backend.trip.dto.TripRequest;
import com.ruben.backend.trip.service.TripService;
import com.ruben.backend.user.dto.RegisterRequest;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.service.AdminAccountInitializer;
import com.ruben.backend.user.service.UserService;
import java.time.LocalDate;
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

@Tag("I3")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ParticipantIntegrationTest {

  @Autowired private ParticipantService participantService;
  @Autowired private InvitationService invitationService;
  @Autowired private TripService tripService;
  @Autowired private UserService userService;
  @Autowired private InvitationRepository invitationRepository;
  @Autowired private ParticipationRepository participationRepository;
  @Autowired private TripMateUserDetailsService userDetailsService;
  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private AdminAccountInitializer adminAccountInitializer;

  private UserDTO ana;
  private UserDTO bob;
  private UserDTO carla;
  private long tripId;

  @BeforeEach
  void setUp() {
    databaseCleaner.clear();
    adminAccountInitializer.run(new DefaultApplicationArguments());
    ana = userService.register(new RegisterRequest("Ana", "ana@example.com", "secret-pass"));
    bob = userService.register(new RegisterRequest("Bob", "bob@example.com", "secret-pass"));
    carla = userService.register(new RegisterRequest("Carla", "carla@example.com", "secret-pass"));
    logInAs(ana);
    TripDTO trip =
        tripService.create(
            new TripRequest(
                "Rome", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 14), null, null));
    tripId = trip.id();
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

  /** Ana invites the user, who accepts; the invitation id is returned. */
  private long join(UserDTO user) {
    logInAs(ana);
    long id = invitationService.create(tripId, new InvitationRequest(user.email())).id();
    logInAs(user);
    invitationService.respond(id, InvitationStatus.ACCEPTED);
    return id;
  }

  @Test
  void theCreatorListsTheParticipantsWithEmailsPagedAndSortedInTheQuery() {
    join(bob);
    join(carla);
    logInAs(ana);

    PageDTO<ParticipantDTO> first =
        participantService.findPage(
            tripId, PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "id")));
    PageDTO<ParticipantDTO> second =
        participantService.findPage(
            tripId, PageRequest.of(1, 1, Sort.by(Sort.Direction.DESC, "id")));

    assertThat(first.totalElements()).isEqualTo(2);
    assertThat(first.last()).isFalse();
    assertThat(first.content())
        .extracting(ParticipantDTO::email)
        .containsExactly("carla@example.com");
    assertThat(second.last()).isTrue();
    assertThat(second.content()).extracting(ParticipantDTO::name).containsExactly("Bob");
    assertThat(second.content().getFirst().joinedAt()).isNotNull();
  }

  @Test
  void aParticipantSeesTheOthersButNeverTheirEmails() {
    join(bob);
    join(carla);
    logInAs(bob);

    PageDTO<ParticipantDTO> page = participantService.findPage(tripId, PageRequest.of(0, 10));

    assertThat(page.content())
        .extracting(ParticipantDTO::name)
        .containsExactlyInAnyOrder("Bob", "Carla");
    assertThat(page.content()).extracting(ParticipantDTO::email).containsOnlyNulls();
  }

  @Test
  void aStrangerMayNotListTheParticipants() {
    join(bob);
    logInAs(carla);

    assertThatThrownBy(() -> participantService.findPage(tripId, PageRequest.of(0, 10)))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void theAvatarIdOfTheProjectionIsTheUuidOfTheStoredImageAndNullWithoutAvatar() {
    join(bob);
    join(carla);
    logInAs(bob);
    UUID stored =
        userService
            .replaceAvatar(
                bob.id(),
                new MockMultipartFile("file", "picture", "image/png", ImageTestData.png(100)))
            .image()
            .id();

    List<ParticipantDTO> participants =
        participantService.findPage(tripId, PageRequest.of(0, 10)).content();

    assertThat(participants)
        .filteredOn(participant -> participant.userId().equals(bob.id()))
        .extracting(ParticipantDTO::avatarId)
        .containsExactly(stored);
    assertThat(participants)
        .filteredOn(participant -> participant.userId().equals(carla.id()))
        .extracting(ParticipantDTO::avatarId)
        .containsOnlyNulls();
  }

  @Test
  void theCreatorRemovesAParticipantWhoLosesAccessAndTheTripLeavesTheirList() {
    join(bob);
    logInAs(bob);
    assertThat(tripService.findPage(PageRequest.of(0, 10)).content()).hasSize(1);
    logInAs(ana);

    participantService.remove(tripId, bob.id());

    assertThat(participationRepository.existsByTripIdAndUserId(tripId, bob.id())).isFalse();
    logInAs(bob);
    assertThat(tripService.findPage(PageRequest.of(0, 10)).content()).isEmpty();
    assertThatThrownBy(() -> tripService.findById(tripId)).isInstanceOf(ForbiddenException.class);
  }

  @Test
  void aParticipantLeavesButCannotRemoveSomeoneElse() {
    join(bob);
    join(carla);
    logInAs(bob);

    assertThatThrownBy(() -> participantService.remove(tripId, carla.id()))
        .isInstanceOf(ForbiddenException.class);
    participantService.remove(tripId, bob.id());

    assertThat(participationRepository.existsByTripIdAndUserId(tripId, bob.id())).isFalse();
    assertThat(participationRepository.existsByTripIdAndUserId(tripId, carla.id())).isTrue();
  }

  @Test
  void theCreatorCannotLeaveAndAMissingParticipantIsNotFound() {
    join(bob);
    logInAs(ana);

    assertThatThrownBy(() -> participantService.remove(tripId, ana.id()))
        .isInstanceOf(ConflictException.class);
    assertThatThrownBy(() -> participantService.remove(tripId, carla.id()))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThat(participationRepository.count()).isEqualTo(1);
  }

  @Test
  void aRemovedUserKeepsTheAcceptedInvitationAndCanBeInvitedAgain() {
    long first = join(bob);
    logInAs(ana);
    participantService.remove(tripId, bob.id());

    Invitation history = invitationRepository.findById(first).orElseThrow();
    assertThat(history.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);

    long second = join(bob);

    assertThat(second).isNotEqualTo(first);
    assertThat(participationRepository.existsByTripIdAndUserId(tripId, bob.id())).isTrue();
    assertThat(participationRepository.count()).isEqualTo(1);
    assertThat(invitationRepository.count()).isEqualTo(2);
    logInAs(bob);
    assertThat(tripService.findById(tripId).name()).isEqualTo("Rome");
  }
}
