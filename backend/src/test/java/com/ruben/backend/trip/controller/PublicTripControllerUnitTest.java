package com.ruben.backend.trip.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ruben.backend.activity.dto.ActivityDTO;
import com.ruben.backend.activity.service.ActivityService;
import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.trip.dto.CopyTripRequest;
import com.ruben.backend.trip.dto.PublicTripDTO;
import com.ruben.backend.trip.dto.PublicTripSummaryDTO;
import com.ruben.backend.trip.dto.TripDTO;
import com.ruben.backend.trip.service.PublicTripService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Tag("B1")
@Tag("I7")
@Tag("I8")
@Tag("I9")
@ExtendWith(MockitoExtension.class)
class PublicTripControllerUnitTest {

  @Mock private PublicTripService publicTripService;
  @Mock private ActivityService activityService;
  @Mock private TripService tripService;
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

  private static TripDTO copy() {
    return new TripDTO(
        9L,
        7L,
        "Rome",
        "Rome",
        LocalDate.of(2027, 8, 1),
        LocalDate.of(2027, 8, 8),
        null,
        null,
        null,
        false,
        null,
        4L,
        0);
  }

  @Test
  void copyPublicTripAnswers201WithTheLocationOfTheNewTrip() {
    TripDTO copy = copy();
    LocalDate start = LocalDate.of(2027, 8, 1);
    when(tripService.copyPublic(4L, start)).thenReturn(copy);

    ResponseEntity<TripDTO> response = controller.copyPublicTrip(4L, new CopyTripRequest(start));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getHeaders().getLocation()).hasToString("/api/v1/trips/9");
    assertThat(response.getBody()).isSameAs(copy);
  }

  @Test
  void copyPublicTripWithoutABodyKeepsTheOriginalDates() {
    when(tripService.copyPublic(4L, null)).thenReturn(copy());

    assertThat(controller.copyPublicTrip(4L, null).getBody()).isEqualTo(copy());
  }

  @Test
  void copyPublicTripWithAnEmptyBodyKeepsTheOriginalDates() {
    when(tripService.copyPublic(4L, null)).thenReturn(copy());

    assertThat(controller.copyPublicTrip(4L, new CopyTripRequest(null)).getBody())
        .isEqualTo(copy());
  }
}
