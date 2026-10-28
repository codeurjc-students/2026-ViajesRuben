package com.ruben.backend.trip.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ruben.backend.activity.dto.ActivityDTO;
import com.ruben.backend.activity.service.ActivityService;
import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.trip.dto.ExampleTripDTO;
import com.ruben.backend.trip.dto.TripDTO;
import com.ruben.backend.trip.service.ExampleTripService;
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
@ExtendWith(MockitoExtension.class)
class ExampleTripControllerUnitTest {

  @Mock private ExampleTripService exampleTripService;
  @Mock private ActivityService activityService;
  @InjectMocks private ExampleTripController controller;

  @Test
  void getExamplesReturnsThePageTheServiceBuilds() {
    Pageable pageable = PageRequest.of(0, 3);
    PageDTO<ExampleTripDTO> page = new PageDTO<>(List.of(), 0, 3, 0, true);
    when(exampleTripService.findPage(pageable)).thenReturn(page);

    assertThat(controller.getExamples(pageable)).isSameAs(page);
  }

  @Test
  void getExampleReturnsTheTrip() {
    TripDTO trip = new TripDTO(4L, 1L, "Rome", "Rome", null, null, null, null, null);
    when(exampleTripService.findById(4L)).thenReturn(trip);

    assertThat(controller.getExample(4L)).isSameAs(trip);
  }

  @Test
  void getExampleActivitiesPassesTheDayAndThePageToTheService() {
    Pageable pageable = PageRequest.of(0, 10);
    LocalDate day = LocalDate.of(2027, 7, 11);
    PageDTO<ActivityDTO> page = new PageDTO<>(List.of(), 0, 10, 0, true);
    when(activityService.findExamplePage(4L, day, pageable)).thenReturn(page);

    assertThat(controller.getExampleActivities(4L, day, pageable)).isSameAs(page);
  }
}
