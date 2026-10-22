package com.ruben.backend.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.trip.dto.TripDTO;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TripServiceUnitTest {

  @Mock private TripRepository tripRepository;
  @InjectMocks private TripService tripService;

  @Test
  void findPageMapsThePageOfTheRepositoryAndKeepsItsPosition() {
    Pageable pageable = PageRequest.of(1, 2);
    when(tripRepository.findAll(pageable))
        .thenReturn(
            new PageImpl<>(
                List.of(
                    new Trip(
                        "Trip 1", "Lisbon", LocalDate.of(2027, 5, 1), LocalDate.of(2027, 5, 5)),
                    new Trip(
                        "Trip 2", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17))),
                pageable,
                4));

    PageDTO<TripDTO> page = tripService.findPage(pageable);

    assertThat(page.content()).extracting(TripDTO::name).containsExactly("Trip 1", "Trip 2");
    assertThat(page.page()).isEqualTo(1);
    assertThat(page.size()).isEqualTo(2);
    assertThat(page.totalElements()).isEqualTo(4);
    assertThat(page.last()).isTrue();
    verify(tripRepository).findAll(pageable);
  }

  @Test
  void findByIdReturnsTheTrip() {
    when(tripRepository.findById(1L))
        .thenReturn(
            Optional.of(
                new Trip("Trip 1", "Lisbon", LocalDate.of(2027, 5, 1), LocalDate.of(2027, 5, 5))));

    TripDTO trip = tripService.findById(1L);

    assertThat(trip.name()).isEqualTo("Trip 1");
    assertThat(trip.destination()).isEqualTo("Lisbon");
  }

  @Test
  void findByIdThrowsWhenTheTripDoesNotExist() {
    when(tripRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tripService.findById(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Trip not found");
  }
}
