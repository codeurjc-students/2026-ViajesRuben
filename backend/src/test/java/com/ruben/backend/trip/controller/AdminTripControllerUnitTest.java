package com.ruben.backend.trip.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.trip.dto.AdminTripDTO;
import com.ruben.backend.trip.service.TripService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Tag("B8")
@ExtendWith(MockitoExtension.class)
class AdminTripControllerUnitTest {

  private static final AdminTripDTO ROME =
      new AdminTripDTO(
          3L,
          "Rome",
          "Italy",
          LocalDate.of(2027, 7, 10),
          LocalDate.of(2027, 7, 17),
          7L,
          "ana@example.com");

  @Mock private TripService tripService;
  @InjectMocks private AdminTripController controller;

  @Test
  void getTripsReturnsThePageOfTheService() {
    Pageable pageable = PageRequest.of(0, 10);
    PageDTO<AdminTripDTO> page = new PageDTO<>(List.of(ROME), 0, 10, 1, true);
    when(tripService.findAllPage("rome", pageable)).thenReturn(page);

    assertThat(controller.getTrips("rome", pageable)).isEqualTo(page);
  }
}
