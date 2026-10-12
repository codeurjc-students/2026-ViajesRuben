package com.ruben.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruben.backend.dto.TripDTO;
import com.ruben.backend.model.Trip;
import com.ruben.backend.repository.TripRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TripServiceUnitTest {

    @Mock
    private TripRepository tripRepository;
    @InjectMocks
    private TripService tripService;

    @Test
    void findAllReturnsAllTripsFromRepository() {
        when(tripRepository.findAll()).thenReturn(List.of(
                new Trip("Trip 1", "Lisbon", LocalDate.of(2027, 5, 1), LocalDate.of(2027, 5, 5)),
                new Trip("Trip 2", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17))));

        List<TripDTO> trips = tripService.findAll();

        assertThat(trips).extracting(TripDTO::name).containsExactly("Trip 1", "Trip 2");
        verify(tripRepository).findAll();
    }
}