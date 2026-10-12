package com.ruben.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.dto.TripDTO;
import com.ruben.backend.model.Trip;
import com.ruben.backend.repository.TripRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TripServiceIntegrationTest {

    @Autowired
    private TripService tripService;
    @Autowired
    private TripRepository tripRepository;

    @BeforeEach
    void setUp() {
        tripRepository.deleteAll();
        tripRepository.saveAll(List.of(
                new Trip("Test Trip A", "Paris", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 3)),
                new Trip("Test Trip B", "Berlin", LocalDate.of(2027, 2, 1), LocalDate.of(2027, 2, 4))));
    }

    @Test
    void findAllReturnsTripsStoredInDatabase() {
        assertThat(tripService.findAll())
                .extracting(TripDTO::name)
                .containsExactlyInAnyOrder("Test Trip A", "Test Trip B");
    }
}