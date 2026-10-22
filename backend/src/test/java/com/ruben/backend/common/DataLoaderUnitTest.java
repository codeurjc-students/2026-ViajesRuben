package com.ruben.backend.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataLoaderUnitTest {

  @Mock private TripRepository tripRepository;
  @InjectMocks private DataLoader dataLoader;
  @Captor private ArgumentCaptor<Iterable<Trip>> saved;

  @Test
  void loadsAllTheSampleTripsIntoAnEmptyDatabase() {
    when(tripRepository.findByNameIn(anyCollection())).thenReturn(List.of());

    dataLoader.run();

    verify(tripRepository).saveAll(saved.capture());
    assertThat(saved.getValue()).hasSize(12);
    assertThat(saved.getValue())
        .extracting(Trip::getDestination)
        .contains("Lisbon", "Rome", "Tokyo");
  }

  @Test
  void onlyAddsTheSampleTripsThatAreMissing() {
    when(tripRepository.findByNameIn(anyCollection()))
        .thenReturn(
            List.of(
                new Trip("Trip 1", "Lisbon", LocalDate.of(2027, 5, 1), LocalDate.of(2027, 5, 5)),
                new Trip("Trip 2", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17))));

    dataLoader.run();

    verify(tripRepository).saveAll(saved.capture());
    assertThat(saved.getValue()).hasSize(10);
    assertThat(saved.getValue()).extracting(Trip::getName).doesNotContain("Trip 1", "Trip 2");
  }
}
