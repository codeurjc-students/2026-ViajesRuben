package com.ruben.backend.invitation.service;

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
import com.ruben.backend.invitation.dto.InvitationDTO;
import com.ruben.backend.invitation.dto.InvitationRequest;
import com.ruben.backend.invitation.dto.ReceivedInvitationDTO;
import com.ruben.backend.invitation.entity.Invitation;
import com.ruben.backend.invitation.entity.InvitationStatus;
import com.ruben.backend.invitation.repository.InvitationRepository;
import com.ruben.backend.participation.repository.ParticipationRepository;
import com.ruben.backend.trip.dto.TripDTO;
import com.ruben.backend.trip.dto.TripRequest;
import com.ruben.backend.trip.service.TripService;
import com.ruben.backend.user.dto.RegisterRequest;
import com.ruben.backend.user.dto.UserDTO;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@Tag("I2")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class InvitationResponseIntegrationTest {

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
    tripId = createTrip("Rome", LocalDate.of(2027, 7, 10)).id();
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

  private TripDTO createTrip(String name, LocalDate start) {
    return tripService.create(new TripRequest(name, "City", start, start.plusDays(4), null, null));
  }

  /** Ana invites the email to the trip and the invitation id is returned. */
  private long invite(String email) {
    logInAs(ana);
    return invitationService.create(tripId, new InvitationRequest(email)).id();
  }

  @Test
  void acceptingStoresTheParticipationAndTheAnswerAndTheTripAppearsInMyTrips() {
    long id = invite("bob@example.com");
    logInAs(bob);

    ReceivedInvitationDTO answered = invitationService.respond(id, InvitationStatus.ACCEPTED);

    assertThat(answered.status()).isEqualTo(InvitationStatus.ACCEPTED);
    assertThat(answered.respondedAt()).isNotNull();
    assertThat(participationRepository.existsByTripIdAndUserId(tripId, bob.id())).isTrue();
    assertThat(invitationRepository.findById(id).orElseThrow().getStatus())
        .isEqualTo(InvitationStatus.ACCEPTED);
    assertThat(tripService.findPage(PageRequest.of(0, 10)).content())
        .extracting(TripDTO::id)
        .containsExactly(tripId);
    assertThat(tripService.findById(tripId).name()).isEqualTo("Rome");
  }

  @Test
  void anInvitationToAnEmailWithoutAnAccountGetsTheAccountWhenItIsAccepted() {
    long id = invite("later@example.com");
    UserDTO later =
        userService.register(new RegisterRequest("Later", "Later@example.com", "secret-pass"));
    logInAs(later);

    invitationService.respond(id, InvitationStatus.ACCEPTED);

    Invitation stored = invitationRepository.findWithDetailsById(id).orElseThrow();
    assertThat(stored.getInvitedUser()).isNotNull();
    assertThat(stored.getInvitedUser().getId()).isEqualTo(later.id());
  }

  @Test
  void rejectingCreatesNoParticipation() {
    long id = invite("bob@example.com");
    logInAs(bob);

    invitationService.respond(id, InvitationStatus.REJECTED);

    assertThat(participationRepository.count()).isZero();
    assertThat(invitationRepository.findById(id).orElseThrow().getStatus())
        .isEqualTo(InvitationStatus.REJECTED);
    assertThat(tripService.findPage(PageRequest.of(0, 10)).content()).isEmpty();
  }

  @Test
  void respondingTwiceIsAConflictAndKeepsTheFirstAnswer() {
    long id = invite("bob@example.com");
    logInAs(bob);
    invitationService.respond(id, InvitationStatus.REJECTED);

    assertThatThrownBy(() -> invitationService.respond(id, InvitationStatus.ACCEPTED))
        .isInstanceOf(ConflictException.class);

    assertThat(participationRepository.count()).isZero();
  }

  @Test
  void anotherUserCannotAnswerOrSeeTheInvitation() {
    long id = invite("bob@example.com");
    String token = invitationRepository.findById(id).orElseThrow().getToken();
    logInAs(carla);

    assertThatThrownBy(() -> invitationService.respond(id, InvitationStatus.ACCEPTED))
        .isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> invitationService.findByToken(token))
        .isInstanceOf(ForbiddenException.class);
    assertThat(participationRepository.count()).isZero();
  }

  @Test
  void findMineListsOnlyMyPendingInvitationsPaginatedAndAnsweredOnesDisappear() {
    logInAs(ana);
    for (int i = 1; i <= 2; i++) {
      long other = createTrip("Trip " + i, LocalDate.of(2027, 8, i)).id();
      invitationService.create(other, new InvitationRequest("bob@example.com"));
    }
    invite("bob@example.com");
    invite("carla@example.com");
    logInAs(bob);

    PageDTO<ReceivedInvitationDTO> first = invitationService.findMine(PageRequest.of(0, 2));

    assertThat(first.totalElements()).isEqualTo(3);
    assertThat(first.content()).hasSize(2);
    assertThat(first.content()).extracting(ReceivedInvitationDTO::inviterName).containsOnly("Ana");

    invitationService.respond(first.content().get(0).id(), InvitationStatus.REJECTED);

    assertThat(invitationService.findMine(PageRequest.of(0, 10)).totalElements()).isEqualTo(2);
  }

  @Test
  void findByTokenFindsTheInvitationOfTheAddresseeInAnyStatus() {
    long id = invite("bob@example.com");
    String token = invitationRepository.findById(id).orElseThrow().getToken();
    logInAs(bob);
    invitationService.respond(id, InvitationStatus.REJECTED);

    ReceivedInvitationDTO found = invitationService.findByToken(token);

    assertThat(found.id()).isEqualTo(id);
    assertThat(found.status()).isEqualTo(InvitationStatus.REJECTED);
    assertThat(found.tripName()).isEqualTo("Rome");
    assertThatThrownBy(() -> invitationService.findByToken("unknown"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void invitingSomeoneWhoAlreadyParticipatesIsAConflict() {
    long id = invite("bob@example.com");
    logInAs(bob);
    invitationService.respond(id, InvitationStatus.ACCEPTED);
    logInAs(ana);
    InvitationRequest again = new InvitationRequest("BOB@example.com");

    assertThatThrownBy(() -> invitationService.create(tripId, again))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("participant");
  }

  @Test
  void aRejectedInvitationCanBeFollowedByANewOne() {
    long id = invite("bob@example.com");
    logInAs(bob);
    invitationService.respond(id, InvitationStatus.REJECTED);
    logInAs(ana);

    InvitationDTO again =
        invitationService.create(tripId, new InvitationRequest("bob@example.com"));

    assertThat(again.status()).isEqualTo(InvitationStatus.PENDING);
  }

  @Test
  void deletingTheTripDeletesItsParticipationsAndInvitations() {
    long accepted = invite("bob@example.com");
    invite("carla@example.com");
    logInAs(bob);
    invitationService.respond(accepted, InvitationStatus.ACCEPTED);
    logInAs(ana);

    tripService.delete(tripId);

    assertThat(participationRepository.count()).isZero();
    assertThat(invitationRepository.count()).isZero();
    logInAs(bob);
    assertThatThrownBy(() -> invitationService.respond(accepted, InvitationStatus.REJECTED))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
