package com.ruben.backend.participation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ruben.backend.common.error.ConflictException;
import com.ruben.backend.participation.dto.TripParticipantCount;
import com.ruben.backend.participation.entity.Participation;
import com.ruben.backend.participation.repository.ParticipationRepository;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@Tag("I2")
@ExtendWith(MockitoExtension.class)
class ParticipationServiceUnitTest {

  @Mock private ParticipationRepository participationRepository;
  @InjectMocks private ParticipationService participationService;

  @Test
  void isParticipantAsksTheRepositoryByTripAndUser() {
    when(participationRepository.existsByTripIdAndUserId(1L, 8L)).thenReturn(true);

    assertThat(participationService.isParticipant(1L, 8L)).isTrue();
    assertThat(participationService.isParticipant(1L, 9L)).isFalse();
  }

  @Test
  void isParticipantByEmailAsksTheRepositoryByTripAndEmail() {
    when(participationRepository.existsByTripIdAndUserEmail(1L, "carlos@example.com"))
        .thenReturn(true);

    assertThat(participationService.isParticipantByEmail(1L, "carlos@example.com")).isTrue();
    assertThat(participationService.isParticipantByEmail(1L, "other@example.com")).isFalse();
  }

  @Test
  void addStoresTheParticipationOfTheUserInTheTrip() {
    User ana = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
    User carlos = UserTestData.user(8L, "carlos@example.com", Role.USER, UserStatus.ACTIVE);
    Trip trip = new Trip(ana, "Trip", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17));

    participationService.add(trip, carlos);

    ArgumentCaptor<Participation> captor = ArgumentCaptor.forClass(Participation.class);
    verify(participationRepository).saveAndFlush(captor.capture());
    assertThat(captor.getValue().getTrip()).isSameAs(trip);
    assertThat(captor.getValue().getUser()).isSameAs(carlos);
  }

  @Test
  void addReportsTheUniqueConstraintOfASimultaneousAcceptAsAConflict() {
    User ana = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
    User carlos = UserTestData.user(8L, "carlos@example.com", Role.USER, UserStatus.ACTIVE);
    Trip trip = new Trip(ana, "Trip", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17));
    when(participationRepository.saveAndFlush(any(Participation.class)))
        .thenThrow(new DataIntegrityViolationException("uk_participation_trip_user"));

    assertThatThrownBy(() -> participationService.add(trip, carlos))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("participant");
  }

  @Test
  @Tag("I3")
  void countByTripAsksTheRepositoryForTheParticipantsOfTheTrip() {
    when(participationRepository.countByTripId(1L)).thenReturn(3L);

    assertThat(participationService.countByTrip(1L)).isEqualTo(3L);
  }

  @Test
  @Tag("I3")
  void countByTripsGivesEveryTripAnEntryWithZeroWhenNobodyJoined() {
    when(participationRepository.countByTripIds(List.of(1L, 2L, 3L)))
        .thenReturn(List.of(new TripParticipantCount(1L, 4L), new TripParticipantCount(3L, 1L)));

    Map<Long, Long> counts = participationService.countByTrips(List.of(1L, 2L, 3L));

    assertThat(counts).containsOnly(entry(1L, 4L), entry(2L, 0L), entry(3L, 1L));
  }

  @Test
  @Tag("I3")
  void countByTripsOfNoTripsAsksNothing() {
    assertThat(participationService.countByTrips(List.of())).isEmpty();

    verifyNoInteractions(participationRepository);
  }

  @Test
  void deleteAllByTripDeletesTheTripsParticipations() {
    participationService.deleteAllByTrip(1L);

    verify(participationRepository).deleteByTripId(1L);
  }
}
