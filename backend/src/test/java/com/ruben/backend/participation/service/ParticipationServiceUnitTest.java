package com.ruben.backend.participation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruben.backend.participation.entity.Participation;
import com.ruben.backend.participation.repository.ParticipationRepository;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
  void deleteAllByTripDeletesTheTripsParticipations() {
    participationService.deleteAllByTrip(1L);

    verify(participationRepository).deleteByTripId(1L);
  }
}
