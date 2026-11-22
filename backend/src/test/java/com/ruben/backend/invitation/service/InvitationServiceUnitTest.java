package com.ruben.backend.invitation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.BadRequestException;
import com.ruben.backend.common.error.ConflictException;
import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.invitation.dto.InvitationDTO;
import com.ruben.backend.invitation.dto.InvitationRequest;
import com.ruben.backend.invitation.dto.ReceivedInvitationDTO;
import com.ruben.backend.invitation.entity.Invitation;
import com.ruben.backend.invitation.entity.InvitationStatus;
import com.ruben.backend.invitation.repository.InvitationRepository;
import com.ruben.backend.notification.event.InvitationCreatedEvent;
import com.ruben.backend.participation.service.ParticipationService;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.service.TripAccessService;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AuthService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("I1")
@ExtendWith(MockitoExtension.class)
class InvitationServiceUnitTest {

  @Mock private InvitationRepository invitationRepository;
  @Mock private UserRepository userRepository;
  @Mock private TripAccessService tripAccessService;
  @Mock private AuthService authService;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private ParticipationService participationService;

  private InvitationService invitationService;
  private User ana;
  private Trip trip;

  @BeforeEach
  void setUp() {
    invitationService =
        new InvitationService(
            invitationRepository,
            userRepository,
            tripAccessService,
            authService,
            eventPublisher,
            participationService);
    ana = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
    trip = new Trip(ana, "Trip", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17));
    ReflectionTestUtils.setField(trip, "id", 1L);
  }

  private void anaIsLoggedIn() {
    when(authService.getCurrentUser()).thenReturn(UserDTO.from(ana));
  }

  private void anaOwnsTheTrip() {
    when(tripAccessService.findForWrite(1L)).thenReturn(trip);
    anaIsLoggedIn();
  }

  private void saveAssignsId(long id) {
    when(invitationRepository.saveAndFlush(any(Invitation.class)))
        .thenAnswer(
            call -> {
              Invitation saved = call.getArgument(0);
              ReflectionTestUtils.setField(saved, "id", id);
              return saved;
            });
    when(userRepository.getReferenceById(7L)).thenReturn(ana);
  }

  private Invitation savedInvitation() {
    ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
    verify(invitationRepository).saveAndFlush(captor.capture());
    return captor.getValue();
  }

  @Test
  void createStoresAPendingInvitationWithANormalisedEmailAndARandomToken() {
    anaOwnsTheTrip();
    saveAssignsId(5L);
    when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

    InvitationDTO dto = invitationService.create(1L, new InvitationRequest("  New@Example.COM "));

    Invitation saved = savedInvitation();
    assertThat(saved.getInvitedEmail()).isEqualTo("new@example.com");
    assertThat(saved.getStatus()).isEqualTo(InvitationStatus.PENDING);
    assertThat(saved.getTrip()).isSameAs(trip);
    assertThat(saved.getInviter()).isSameAs(ana);
    assertThat(saved.getInvitedUser()).isNull();
    assertThat(saved.getToken()).hasSize(43).matches("[A-Za-z0-9_-]+");
    assertThat(dto.id()).isEqualTo(5L);
    assertThat(dto.invitedEmail()).isEqualTo("new@example.com");
    assertThat(dto.status()).isEqualTo(InvitationStatus.PENDING);
    assertThat(dto.sentAt()).isNotNull();
  }

  @Test
  void createPublishesTheEventThatSendsTheEmailWithWhatTheEmailNeeds() {
    anaOwnsTheTrip();
    saveAssignsId(5L);

    invitationService.create(1L, new InvitationRequest("New@Example.com"));

    ArgumentCaptor<InvitationCreatedEvent> captor =
        ArgumentCaptor.forClass(InvitationCreatedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    InvitationCreatedEvent event = captor.getValue();
    assertThat(event.invitationId()).isEqualTo(5L);
    assertThat(event.invitedEmail()).isEqualTo("new@example.com");
    assertThat(event.token()).isEqualTo(savedInvitation().getToken());
    assertThat(event.inviterName()).isEqualTo(ana.getName());
    assertThat(event.tripName()).isEqualTo("Trip");
  }

  @Test
  void createPublishesNothingWhenTheInvitationIsRefused() {
    anaOwnsTheTrip();
    when(invitationRepository.existsByTripIdAndInvitedEmailAndStatus(
            1L, "new@example.com", InvitationStatus.PENDING))
        .thenReturn(true);

    assertThatThrownBy(() -> invitationService.create(1L, new InvitationRequest("new@example.com")))
        .isInstanceOf(ConflictException.class);
    assertThatThrownBy(() -> invitationService.create(1L, new InvitationRequest("ana@example.com")))
        .isInstanceOf(BadRequestException.class);

    verifyNoInteractions(eventPublisher);
  }

  @Test
  void createPublishesNothingWhenTheUserMayNotInvite() {
    when(tripAccessService.findForWrite(1L)).thenThrow(new ForbiddenException("no"));

    assertThatThrownBy(() -> invitationService.create(1L, new InvitationRequest("x@example.com")))
        .isInstanceOf(ForbiddenException.class);

    verifyNoInteractions(eventPublisher);
  }

  @Test
  void createLinksTheAccountThatOwnsTheEmail() {
    User carlos = UserTestData.user(8L, "carlos@example.com", Role.USER, UserStatus.ACTIVE);
    anaOwnsTheTrip();
    saveAssignsId(5L);
    when(userRepository.findByEmail("carlos@example.com")).thenReturn(Optional.of(carlos));

    invitationService.create(1L, new InvitationRequest("Carlos@example.com"));

    assertThat(savedInvitation().getInvitedUser()).isSameAs(carlos);
  }

  @Test
  void everyInvitationGetsItsOwnToken() {
    assertThat(InvitationService.newToken()).isNotEqualTo(InvitationService.newToken());
  }

  @Test
  void createRefusesTheCreatorsOwnEmailIgnoringCase() {
    anaOwnsTheTrip();

    assertThatThrownBy(
            () -> invitationService.create(1L, new InvitationRequest(" ANA@example.com")))
        .isInstanceOf(BadRequestException.class);

    verify(invitationRepository, never()).saveAndFlush(any());
  }

  @Test
  void createRefusesAnEmailWithAPendingInvitation() {
    anaOwnsTheTrip();
    when(invitationRepository.existsByTripIdAndInvitedEmailAndStatus(
            1L, "new@example.com", InvitationStatus.PENDING))
        .thenReturn(true);

    assertThatThrownBy(() -> invitationService.create(1L, new InvitationRequest("New@example.com")))
        .isInstanceOf(ConflictException.class);

    verify(invitationRepository, never()).saveAndFlush(any());
  }

  @Test
  void createPropagatesTheAccessRefusal() {
    when(tripAccessService.findForWrite(1L)).thenThrow(new ForbiddenException("no"));

    assertThatThrownBy(() -> invitationService.create(1L, new InvitationRequest("x@example.com")))
        .isInstanceOf(ForbiddenException.class);

    verifyNoInteractions(invitationRepository);
  }

  @Test
  void createPropagatesAMissingTrip() {
    when(tripAccessService.findForWrite(1L)).thenThrow(new ResourceNotFoundException("no"));

    assertThatThrownBy(() -> invitationService.create(1L, new InvitationRequest("x@example.com")))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void findPageReturnsTheTripsInvitationsAsDtos() {
    when(tripAccessService.findForWrite(1L)).thenReturn(trip);
    Invitation invitation = new Invitation(trip, ana, "new@example.com", null, "token");
    ReflectionTestUtils.setField(invitation, "id", 5L);
    Pageable pageable = PageRequest.of(0, 10, Sort.by("sentAt").descending());
    when(invitationRepository.findByTripId(1L, pageable))
        .thenReturn(new PageImpl<>(List.of(invitation), pageable, 1));

    PageDTO<InvitationDTO> page = invitationService.findPage(1L, pageable);

    assertThat(page.content())
        .extracting(InvitationDTO::invitedEmail)
        .containsExactly("new@example.com");
    assertThat(page.totalElements()).isEqualTo(1);
    assertThat(page.last()).isTrue();
  }

  @Test
  void findPageRefusesToSortByAPropertyThatIsNotSortable() {
    Pageable pageable = PageRequest.of(0, 10, Sort.by("token"));

    assertThatThrownBy(() -> invitationService.findPage(1L, pageable))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("token");

    verifyNoInteractions(invitationRepository);
  }

  @Test
  void findPagePropagatesTheAccessRefusal() {
    when(tripAccessService.findForWrite(1L)).thenThrow(new ForbiddenException("no"));

    assertThatThrownBy(() -> invitationService.findPage(1L, PageRequest.of(0, 10)))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void findByIdReturnsTheInvitationOfTheTrip() {
    when(tripAccessService.findForWrite(1L)).thenReturn(trip);
    when(invitationRepository.findByIdAndTripId(5L, 1L))
        .thenReturn(Optional.of(new Invitation(trip, ana, "new@example.com", null, "token")));

    assertThat(invitationService.findById(1L, 5L).invitedEmail()).isEqualTo("new@example.com");
  }

  @Test
  void findByIdDoesNotFindAnInvitationOfAnotherTrip() {
    when(tripAccessService.findForWrite(1L)).thenReturn(trip);
    when(invitationRepository.findByIdAndTripId(5L, 1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> invitationService.findById(1L, 5L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void findByIdPropagatesTheAccessRefusal() {
    when(tripAccessService.findForWrite(1L)).thenThrow(new ForbiddenException("no"));

    assertThatThrownBy(() -> invitationService.findById(1L, 5L))
        .isInstanceOf(ForbiddenException.class);

    verifyNoInteractions(invitationRepository);
  }

  @Test
  void deleteAllByTripDeletesTheTripsInvitations() {
    invitationService.deleteAllByTrip(1L);

    verify(invitationRepository).deleteByTripId(1L);
  }

  // ---- I2: the invited person answers ----

  private User carlos;

  private void carlosIsLoggedIn() {
    carlos = UserTestData.user(8L, "carlos@example.com", Role.USER, UserStatus.ACTIVE);
    when(authService.getCurrentUser()).thenReturn(UserDTO.from(carlos));
  }

  private Invitation invitationFor(String email) {
    Invitation invitation = new Invitation(trip, ana, email, null, "token-1");
    ReflectionTestUtils.setField(invitation, "id", 5L);
    return invitation;
  }

  @Test
  @Tag("I2")
  void createRefusesAnEmailThatAlreadyParticipatesWithA409() {
    anaOwnsTheTrip();
    when(participationService.isParticipantByEmail(1L, "carlos@example.com")).thenReturn(true);

    assertThatThrownBy(
            () -> invitationService.create(1L, new InvitationRequest("Carlos@example.com")))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("participant");

    verify(invitationRepository, never()).saveAndFlush(any());
    verifyNoInteractions(eventPublisher);
  }

  @Test
  @Tag("I2")
  void findMineListsThePendingInvitationsOfTheNormalisedEmailOfTheUser() {
    when(authService.getCurrentUser())
        .thenReturn(
            UserDTO.from(
                UserTestData.user(8L, " Carlos@Example.com ", Role.USER, UserStatus.ACTIVE)));
    Pageable pageable = PageRequest.of(0, 10, Sort.by("sentAt").descending());
    when(invitationRepository.findByInvitedEmailAndStatus(
            "carlos@example.com", InvitationStatus.PENDING, pageable))
        .thenReturn(new PageImpl<>(List.of(invitationFor("carlos@example.com")), pageable, 1));

    PageDTO<ReceivedInvitationDTO> page = invitationService.findMine(pageable);

    assertThat(page.content()).hasSize(1);
    assertThat(page.content().get(0).tripName()).isEqualTo("Trip");
    assertThat(page.content().get(0).inviterName()).isEqualTo(ana.getName());
    assertThat(page.totalElements()).isEqualTo(1);
  }

  @Test
  @Tag("I2")
  void findMineRefusesToSortByAPropertyThatIsNotSortable() {
    Pageable pageable = PageRequest.of(0, 10, Sort.by("invitedEmail"));

    assertThatThrownBy(() -> invitationService.findMine(pageable))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("invitedEmail");

    verifyNoInteractions(invitationRepository);
  }

  @Test
  @Tag("I2")
  void findByTokenReturnsTheInvitationToItsAddressee() {
    carlosIsLoggedIn();
    when(invitationRepository.findByToken("token-1"))
        .thenReturn(Optional.of(invitationFor("carlos@example.com")));

    ReceivedInvitationDTO dto = invitationService.findByToken("token-1");

    assertThat(dto.id()).isEqualTo(5L);
    assertThat(dto.tripId()).isEqualTo(1L);
    assertThat(dto.status()).isEqualTo(InvitationStatus.PENDING);
  }

  @Test
  @Tag("I2")
  void findByTokenReturnsAnAnsweredInvitationToo() {
    carlosIsLoggedIn();
    Invitation invitation = invitationFor("carlos@example.com");
    invitation.reject();
    when(invitationRepository.findByToken("token-1")).thenReturn(Optional.of(invitation));

    assertThat(invitationService.findByToken("token-1").status())
        .isEqualTo(InvitationStatus.REJECTED);
  }

  @Test
  @Tag("I2")
  void findByTokenRefusesAnotherAccountWithoutRevealingAnything() {
    anaIsLoggedIn();
    when(invitationRepository.findByToken("token-1"))
        .thenReturn(Optional.of(invitationFor("carlos@example.com")));

    assertThatThrownBy(() -> invitationService.findByToken("token-1"))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  @Tag("I2")
  void findByTokenDoesNotFindAnUnknownToken() {
    when(invitationRepository.findByToken("nope")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> invitationService.findByToken("nope"))
        .isInstanceOf(ResourceNotFoundException.class);

    verifyNoInteractions(authService);
  }

  @Test
  @Tag("I2")
  void acceptingMakesTheUserAParticipantAndRemembersTheAccount() {
    carlosIsLoggedIn();
    Invitation invitation = invitationFor("carlos@example.com");
    when(invitationRepository.findWithDetailsById(5L)).thenReturn(Optional.of(invitation));
    when(participationService.isParticipant(1L, 8L)).thenReturn(false);
    when(userRepository.getReferenceById(8L)).thenReturn(carlos);

    ReceivedInvitationDTO dto = invitationService.respond(5L, InvitationStatus.ACCEPTED);

    verify(participationService).add(trip, carlos);
    verify(invitationRepository).saveAndFlush(invitation);
    assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
    assertThat(invitation.getInvitedUser()).isSameAs(carlos);
    assertThat(invitation.getRespondedAt()).isNotNull();
    assertThat(dto.status()).isEqualTo(InvitationStatus.ACCEPTED);
    assertThat(dto.respondedAt()).isEqualTo(invitation.getRespondedAt());
  }

  @Test
  @Tag("I2")
  void rejectingAddsNoParticipant() {
    carlosIsLoggedIn();
    Invitation invitation = invitationFor("carlos@example.com");
    when(invitationRepository.findWithDetailsById(5L)).thenReturn(Optional.of(invitation));

    ReceivedInvitationDTO dto = invitationService.respond(5L, InvitationStatus.REJECTED);

    verifyNoInteractions(participationService);
    assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.REJECTED);
    assertThat(invitation.getRespondedAt()).isNotNull();
    assertThat(dto.status()).isEqualTo(InvitationStatus.REJECTED);
  }

  @Test
  @Tag("I2")
  void respondRefusesAnotherAccountAndChangesNothing() {
    anaIsLoggedIn();
    Invitation invitation = invitationFor("carlos@example.com");
    when(invitationRepository.findWithDetailsById(5L)).thenReturn(Optional.of(invitation));

    assertThatThrownBy(() -> invitationService.respond(5L, InvitationStatus.ACCEPTED))
        .isInstanceOf(ForbiddenException.class);

    verifyNoInteractions(participationService);
    verify(invitationRepository, never()).saveAndFlush(any());
    assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
  }

  @Test
  @Tag("I2")
  void respondingTwiceIsAConflict() {
    carlosIsLoggedIn();
    Invitation invitation = invitationFor("carlos@example.com");
    invitation.reject();
    when(invitationRepository.findWithDetailsById(5L)).thenReturn(Optional.of(invitation));

    assertThatThrownBy(() -> invitationService.respond(5L, InvitationStatus.ACCEPTED))
        .isInstanceOf(ConflictException.class);

    verifyNoInteractions(participationService);
    assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.REJECTED);
  }

  @Test
  @Tag("I2")
  void acceptingWhenAlreadyAParticipantIsAConflict() {
    carlosIsLoggedIn();
    Invitation invitation = invitationFor("carlos@example.com");
    when(invitationRepository.findWithDetailsById(5L)).thenReturn(Optional.of(invitation));
    when(participationService.isParticipant(1L, 8L)).thenReturn(true);

    assertThatThrownBy(() -> invitationService.respond(5L, InvitationStatus.ACCEPTED))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("participant");

    verify(participationService, never()).add(any(), any());
    assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
  }

  @Test
  @Tag("I2")
  void theCreatorCannotAcceptAnInvitationToTheirOwnTrip() {
    anaIsLoggedIn();
    Invitation invitation = invitationFor("ana@example.com");
    when(invitationRepository.findWithDetailsById(5L)).thenReturn(Optional.of(invitation));

    assertThatThrownBy(() -> invitationService.respond(5L, InvitationStatus.ACCEPTED))
        .isInstanceOf(ConflictException.class);

    verify(participationService, never()).add(any(), any());
  }

  @Test
  @Tag("I2")
  void respondRefusesPendingAsAnAnswer() {
    assertThatThrownBy(() -> invitationService.respond(5L, InvitationStatus.PENDING))
        .isInstanceOf(BadRequestException.class);

    verifyNoInteractions(invitationRepository);
  }

  @Test
  @Tag("I2")
  void respondDoesNotFindAnUnknownOrDeletedInvitation() {
    when(invitationRepository.findWithDetailsById(5L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> invitationService.respond(5L, InvitationStatus.ACCEPTED))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @Tag("I2")
  void theAddresseeIsMatchedIgnoringTheCaseOfTheirAccountEmail() {
    when(authService.getCurrentUser())
        .thenReturn(
            UserDTO.from(
                UserTestData.user(8L, "Carlos@Example.com", Role.USER, UserStatus.ACTIVE)));
    when(invitationRepository.findByToken("token-1"))
        .thenReturn(Optional.of(invitationFor("carlos@example.com")));

    assertThat(invitationService.findByToken("token-1").id()).isEqualTo(5L);
  }
}
