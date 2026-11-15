package com.ruben.backend.trip.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ruben.backend.activity.dto.ActivityDTO;
import com.ruben.backend.activity.service.ActivityService;
import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.trip.dto.PublicTripDTO;
import com.ruben.backend.trip.dto.PublicTripSummaryDTO;
import com.ruben.backend.trip.service.PublicTripService;
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

@Tag("B1")
@Tag("I7")
@Tag("I8")
@ExtendWith(MockitoExtension.class)
class PublicTripControllerUnitTest {

  @Mock private PublicTripService publicTripService;
  @Mock private ActivityService activityService;
  @InjectMocks private PublicTripController controller;

  @Test
  void getPublicTripsReturnsThePageTheServiceBuilds() {
    Pageable pageable = PageRequest.of(0, 3);
    PageDTO<PublicTripSummaryDTO> page = new PageDTO<>(List.of(), 0, 3, 0, true);
    when(publicTripService.findPage(null, pageable)).thenReturn(page);

    assertThat(controller.getPublicTrips(null, pageable)).isSameAs(page);
  }

  @Test
  void getPublicTripsPassesTheSearchTextToTheService() {
    Pageable pageable = PageRequest.of(0, 10);
    PageDTO<PublicTripSummaryDTO> page = new PageDTO<>(List.of(), 0, 10, 0, true);
    when(publicTripService.findPage("rome", pageable)).thenReturn(page);

    assertThat(controller.getPublicTrips("rome", pageable)).isSameAs(page);
  }

  @Test
  void getPublicTripReturnsTheTrip() {
    PublicTripDTO trip =
        new PublicTripDTO(4L, "Rome", "Rome", null, null, null, null, null, "Ana", null);
    when(publicTripService.findById(4L)).thenReturn(trip);

    assertThat(controller.getPublicTrip(4L)).isSameAs(trip);
  }

  @Test
  void getPublicTripActivitiesPassesTheDayAndThePageToTheService() {
    Pageable pageable = PageRequest.of(0, 10);
    LocalDate day = LocalDate.of(2027, 7, 11);
    PageDTO<ActivityDTO> page = new PageDTO<>(List.of(), 0, 10, 0, true);
    when(activityService.findPublicPage(4L, day, pageable)).thenReturn(page);

    assertThat(controller.getPublicTripActivities(4L, day, pageable)).isSameAs(page);
  }
}
