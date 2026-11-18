package com.ruben.backend.invitation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.BadRequestException;
import com.ruben.backend.common.error.ConflictException;
import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.common.security.TripMateUserDetails;
import com.ruben.backend.common.security.TripMateUserDetailsService;
import com.ruben.backend.invitation.dto.InvitationDTO;
import com.ruben.backend.invitation.dto.InvitationRequest;
import com.ruben.backend.invitation.entity.Invitation;
import com.ruben.backend.invitation.entity.InvitationStatus;
import com.ruben.backend.invitation.repository.InvitationRepository;
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
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@Tag("I1")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class InvitationServiceIntegrationTest {

  @Autowired private InvitationService invitationService;
  @Autowired private TripService tripService;
  @Autowired private UserService userService;
  @Autowired private InvitationRepository invitationRepository;
  @Autowired private TripMateUserDetailsService userDetailsService;
  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private AdminAccountInitializer adminAccountInitializer;

  private UserDTO ana;
  private UserDTO bob;
  private long tripId;

  @BeforeEach
  void setUp() {
    databaseCleaner.clear();
    adminAccountInitializer.run(new DefaultApplicationArguments());
    ana = userService.register(new RegisterRequest("Ana", "ana@example.com", "secret-pass"));
    bob = userService.register(new RegisterRequest("Bob", "bob@example.com", "secret-pass"));
    logInAs(ana);
    tripId = createTrip();
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

  private long createTrip() {
    LocalDate start = LocalDate.of(2027, 7, 10);
    TripDTO trip =
        tripService.create(new TripRequest("Rome", "Rome", start, start.plusDays(4), null, null));
    return trip.id();
  }

  private InvitationDTO invite(String email) {
    return invitationService.create(tripId, new InvitationRequest(email));
  }

  @Test
  void createPersistsAPendingInvitationWithAnUnguessableUniqueToken() {
    InvitationDTO first = invite("  Friend@Example.com ");
    InvitationDTO second = invite("other@example.com");

    Invitation stored = invitationRepository.findById(first.id()).orElseThrow();
    Invitation other = invitationRepository.findById(second.id()).orElseThrow();
    assertThat(stored.getInvitedEmail()).isEqualTo("friend@example.com");
    assertThat(stored.getStatus()).isEqualTo(InvitationStatus.PENDING);
    assertThat(stored.getSentAt()).isNotNull();
    assertThat(stored.getToken()).hasSize(43).isNotEqualTo(other.getToken());
    assertThat(stored.getInvitedUser()).isNull();
  }

  @Test
  void createLinksTheAccountThatOwnsTheEmail() {
    InvitationDTO invitation = invite("BOB@example.com");

    Invitation stored = invitationRepository.findById(invitation.id()).orElseThrow();
    assertThat(stored.getInvitedUser().getId()).isEqualTo(bob.id());
  }

  @Test
  void aSecondPendingInvitationForTheSameEmailIsAConflictEvenWrittenDifferently() {
    invite("friend@example.com");

    assertThatThrownBy(() -> invite(" FRIEND@example.com")).isInstanceOf(ConflictException.class);
    assertThat(invitationRepository.count()).isEqualTo(1);
  }

  @Test
  void theSameEmailCanBeInvitedToAnotherTrip() {
    invite("friend@example.com");
    long otherTrip = createTrip();

    invitationService.create(otherTrip, new InvitationRequest("friend@example.com"));

    assertThat(invitationRepository.count()).isEqualTo(2);
  }

  @Test
  void theCreatorCannotInviteTheirOwnEmail() {
    assertThatThrownBy(() -> invite("Ana@example.com")).isInstanceOf(BadRequestException.class);
    assertThat(invitationRepository.count()).isZero();
  }

  @Test
  void onlyTheCreatorCanInviteAndList() {
    invite("friend@example.com");
    logInAs(bob);

    assertThatThrownBy(() -> invite("other@example.com")).isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> invitationService.findPage(tripId, PageRequest.of(0, 10)))
        .isInstanceOf(ForbiddenException.class);
    assertThat(invitationRepository.count()).isEqualTo(1);
  }

  @Test
  void anUnknownTripIsNotFound() {
    assertThatThrownBy(() -> invitationService.create(999_999L, new InvitationRequest("a@b.com")))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void findPageIsPaginatedAndSortedInTheQuery() {
    for (int i = 1; i <= 12; i++) {
      invite("friend%02d@example.com".formatted(i));
    }
    Sort byEmail = Sort.by("invitedEmail");

    PageDTO<InvitationDTO> first =
        invitationService.findPage(tripId, PageRequest.of(0, 10, byEmail));
    PageDTO<InvitationDTO> second =
        invitationService.findPage(tripId, PageRequest.of(1, 10, byEmail));

    assertThat(first.content()).hasSize(10);
    assertThat(first.totalElements()).isEqualTo(12);
    assertThat(first.last()).isFalse();
    assertThat(first.content().getFirst().invitedEmail()).isEqualTo("friend01@example.com");
    assertThat(second.content())
        .extracting(InvitationDTO::invitedEmail)
        .containsExactly("friend11@example.com", "friend12@example.com");
    assertThat(second.last()).isTrue();
  }

  @Test
  void findByIdOnlyFindsInvitationsOfThatTrip() {
    InvitationDTO invitation = invite("friend@example.com");
    long otherTrip = createTrip();

    assertThat(invitationService.findById(tripId, invitation.id()).invitedEmail())
        .isEqualTo("friend@example.com");
    assertThatThrownBy(() -> invitationService.findById(otherTrip, invitation.id()))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void deletingTheTripDeletesItsInvitationsAndOnlyThose() {
    invite("friend@example.com");
    invite("other@example.com");
    long otherTrip = createTrip();
    invitationService.create(otherTrip, new InvitationRequest("friend@example.com"));

    tripService.delete(tripId);

    assertThat(invitationRepository.findAll())
        .extracting(invitation -> invitation.getTrip().getId())
        .containsExactly(otherTrip);
  }
}
