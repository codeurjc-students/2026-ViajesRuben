package com.ruben.backend.participation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.BadRequestException;
import com.ruben.backend.common.error.ConflictException;
import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.participation.dto.ParticipantDTO;
import com.ruben.backend.participation.entity.Participation;
import com.ruben.backend.participation.repository.ParticipationRepository;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.service.TripAccessService;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import com.ruben.backend.user.service.AuthService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Tag("I3")
@ExtendWith(MockitoExtension.class)
class ParticipantServiceUnitTest {

  private static final Pageable PAGE = PageRequest.of(0, 10, Sort.by("joinedAt", "id"));

  @Mock private ParticipationRepository participationRepository;
  @Mock private TripAccessService tripAccessService;
  @Mock private AuthService authService;

  private ParticipantService participantService;
  private User ana;
  private User bob;
  private Trip trip;

  @BeforeEach
  void setUp() {
    participantService =
        new ParticipantService(participationRepository, tripAccessService, authService);
    ana = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
    bob = UserTestData.user(8L, "bob@example.com", Role.USER, UserStatus.ACTIVE);
    trip = new Trip(ana, "Trip", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17));
  }

  private void loggedInAs(User user) {
    when(authService.getCurrentUser()).thenReturn(UserDTO.from(user));
  }

  private static ParticipantDTO participant(Long userId, String email, UUID avatarId) {
    return new ParticipantDTO(
        userId, "User " + userId, avatarId, email, Instant.parse("2027-01-01T10:00:00Z"));
  }

  @Test
  void theCreatorSeesTheParticipantsWithTheirEmails() {
    loggedInAs(ana);
    UUID avatar = UUID.randomUUID();
    ParticipantDTO bobDto = participant(8L, "bob@example.com", avatar);
    when(tripAccessService.findForRead(1L)).thenReturn(trip);
    when(participationRepository.findParticipantsByTripId(1L, PAGE))
        .thenReturn(new PageImpl<>(List.of(bobDto), PAGE, 1));

    PageDTO<ParticipantDTO> page = participantService.findPage(1L, PAGE);

    assertThat(page.content()).containsExactly(bobDto);
    assertThat(page.content().getFirst().email()).isEqualTo("bob@example.com");
    assertThat(page.content().getFirst().avatarId()).isEqualTo(avatar);
    assertThat(page.totalElements()).isEqualTo(1);
  }

  @Test
  void aParticipantSeesTheOthersWithoutEmailsAndKeepsTheAvatarIds() {
    loggedInAs(bob);
    UUID avatar = UUID.randomUUID();
    ParticipantDTO carlos = participant(9L, "carlos@example.com", avatar);
    when(tripAccessService.findForRead(1L)).thenReturn(trip);
    when(participationRepository.findParticipantsByTripId(1L, PAGE))
        .thenReturn(new PageImpl<>(List.of(carlos), PAGE, 1));

    PageDTO<ParticipantDTO> page = participantService.findPage(1L, PAGE);

    ParticipantDTO seen = page.content().getFirst();
    assertThat(seen.email()).isNull();
    assertThat(seen.userId()).isEqualTo(9L);
    assertThat(seen.name()).isEqualTo("User 9");
    assertThat(seen.avatarId()).isEqualTo(avatar);
    assertThat(seen.joinedAt()).isEqualTo(carlos.joinedAt());
  }

  @Test
  void aParticipantWithoutAvatarHasANullAvatarId() {
    loggedInAs(ana);
    when(tripAccessService.findForRead(1L)).thenReturn(trip);
    when(participationRepository.findParticipantsByTripId(1L, PAGE))
        .thenReturn(new PageImpl<>(List.of(participant(8L, "bob@example.com", null)), PAGE, 1));

    assertThat(participantService.findPage(1L, PAGE).content().getFirst().avatarId()).isNull();
  }

  @Test
  void listingRefusesToSortByAPropertyThatIsNotSortable() {
    Pageable bad = PageRequest.of(0, 10, Sort.by("user.password"));

    assertThatThrownBy(() -> participantService.findPage(1L, bad))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Cannot sort by 'user.password'");
    verifyNoInteractions(tripAccessService, participationRepository);
  }

  @Test
  void listingIsRefusedWhenTheUserMayNotSeeTheTrip() {
    when(tripAccessService.findForRead(1L)).thenThrow(new ForbiddenException("no"));

    assertThatThrownBy(() -> participantService.findPage(1L, PAGE))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(participationRepository);
  }

  @Test
  void theCreatorRemovesAParticipant() {
    loggedInAs(ana);
    Participation participation = new Participation(trip, bob);
    when(tripAccessService.findForParticipantRemoval(1L, 8L)).thenReturn(trip);
    when(participationRepository.findByTripIdAndUserId(1L, 8L))
        .thenReturn(Optional.of(participation));

    participantService.remove(1L, 8L);

    InOrder order = inOrder(participationRepository);
    order.verify(participationRepository).delete(participation);
    order.verify(participationRepository).flush();
  }

  @Test
  void aParticipantLeavesTheTrip() {
    loggedInAs(bob);
    Participation participation = new Participation(trip, bob);
    when(tripAccessService.findForParticipantRemoval(1L, 8L)).thenReturn(trip);
    when(participationRepository.findByTripIdAndUserId(1L, 8L))
        .thenReturn(Optional.of(participation));

    participantService.remove(1L, 8L);

    verify(participationRepository).delete(participation);
  }

  @Test
  void theCreatorCannotLeaveTheirOwnTrip() {
    loggedInAs(ana);
    when(tripAccessService.findForParticipantRemoval(1L, 7L)).thenReturn(trip);

    assertThatThrownBy(() -> participantService.remove(1L, 7L))
        .isInstanceOf(ConflictException.class)
        .hasMessage("The creator cannot leave the trip; delete it instead");
    verify(participationRepository, never()).delete(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void removingSomeoneWhoDoesNotParticipateIsNotFound() {
    loggedInAs(ana);
    when(tripAccessService.findForParticipantRemoval(1L, 9L)).thenReturn(trip);
    when(participationRepository.findByTripIdAndUserId(1L, 9L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> participantService.remove(1L, 9L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Participant not found");
    verify(participationRepository, never()).delete(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void removingIsRefusedWhenTheAccessRuleRefusesIt() {
    when(tripAccessService.findForParticipantRemoval(1L, 9L))
        .thenThrow(new ForbiddenException("no"));

    assertThatThrownBy(() -> participantService.remove(1L, 9L))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(participationRepository);
  }
}
