package com.ruben.backend.trip.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.trip.dto.TripDTO;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TripServiceIntegrationTest {

  @Autowired private TripService tripService;
  @Autowired private TripRepository tripRepository;

  @BeforeEach
  void setUp() {
    tripRepository.deleteAll();
    List<Trip> trips = new ArrayList<>();
    for (int i = 1; i <= 12; i++) {
      trips.add(
          new Trip(
              "Test Trip " + i,
              "City " + i,
              LocalDate.of(2027, 1, 1).plusDays(i),
              LocalDate.of(2027, 1, 3).plusDays(i)));
    }
    tripRepository.saveAll(trips);
  }

  @Test
  void findPageReturnsTheFirstTenTripsOrderedByStartDate() {
    PageDTO<TripDTO> page = tripService.findPage(PageRequest.of(0, 10, Sort.by("startDate")));

    assertThat(page.content()).hasSize(10);
    assertThat(page.content().get(0).name()).isEqualTo("Test Trip 1");
    assertThat(page.content().get(9).name()).isEqualTo("Test Trip 10");
    assertThat(page.totalElements()).isEqualTo(12);
    assertThat(page.last()).isFalse();
  }

  @Test
  void findPageReturnsTheRestOnTheSecondPage() {
    PageDTO<TripDTO> page = tripService.findPage(PageRequest.of(1, 10, Sort.by("startDate")));

    assertThat(page.content())
        .extracting(TripDTO::name)
        .containsExactly("Test Trip 11", "Test Trip 12");
    assertThat(page.last()).isTrue();
  }
}
