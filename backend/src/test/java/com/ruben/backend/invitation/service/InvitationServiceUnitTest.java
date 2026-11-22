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
import com.ruben.backend.invitation.entity.Invitation;
import com.ruben.backend.invitation.entity.InvitationStatus;
import com.ruben.backend.invitation.repository.InvitationRepository;
import com.ruben.backend.notification.event.InvitationCreatedEvent;
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

  private InvitationService invitationService;
  private User ana;
  private Trip trip;

  @BeforeEach
  void setUp() {
    invitationService =
        new InvitationService(
            invitationRepository, userRepository, tripAccessService, authService, eventPublisher);
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
}
