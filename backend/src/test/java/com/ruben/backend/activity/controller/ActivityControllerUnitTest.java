package com.ruben.backend.activity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruben.backend.activity.dto.ActivityDTO;
import com.ruben.backend.activity.dto.ActivityRequest;
import com.ruben.backend.activity.dto.TripCostDTO;
import com.ruben.backend.activity.entity.ActivityCategory;
import com.ruben.backend.activity.service.ActivityService;
import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.image.dto.ImageContentDTO;
import com.ruben.backend.image.dto.ImageDTO;
import com.ruben.backend.image.dto.ImageResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

@Tag("B5")
@ExtendWith(MockitoExtension.class)
class ActivityControllerUnitTest {

  private static final LocalDate DAY = LocalDate.of(2027, 7, 11);
  private static final ActivityDTO ACTIVITY =
      new ActivityDTO(
          5L,
          3L,
          7L,
          "Colosseum",
          null,
          null,
          DAY,
          LocalTime.of(9, 0),
          LocalTime.of(11, 0),
          new BigDecimal("16.00"),
          ActivityCategory.SIGHTSEEING,
          null);
  private static final ActivityRequest REQUEST =
      new ActivityRequest("Colosseum", DAY, null, null, null, null, null, null);
  private static final MockMultipartFile PICTURE =
      new MockMultipartFile("file", "a.png", "image/png", ImageTestData.png(100));
  private static final ImageDTO IMAGE = new ImageDTO(UUID.randomUUID(), "image/png", 100);

  @Mock private ActivityService activityService;
  @InjectMocks private ActivityController controller;

  @Test
  void getCostReturnsWhatTheServiceCalculatesForTheTrip() {
    TripCostDTO cost = new TripCostDTO(new BigDecimal("30.00"), new BigDecimal("70.00"));
    when(activityService.findCost(3L)).thenReturn(cost);

    assertThat(controller.getCost(3L)).isSameAs(cost);
  }

  @Test
  void getActivitiesReturnsThePageOfTheServiceForTheDay() {
    Pageable pageable = PageRequest.of(0, 10);
    PageDTO<ActivityDTO> page = new PageDTO<>(List.of(ACTIVITY), 0, 10, 1, true);
    when(activityService.findPage(3L, DAY, pageable)).thenReturn(page);

    assertThat(controller.getActivities(3L, DAY, pageable)).isEqualTo(page);
  }

  @Test
  void getActivitiesWithoutDayAsksForTheWholeItinerary() {
    Pageable pageable = PageRequest.of(0, 10);
    PageDTO<ActivityDTO> page = new PageDTO<>(List.of(), 0, 10, 0, true);
    when(activityService.findPage(3L, null, pageable)).thenReturn(page);

    assertThat(controller.getActivities(3L, null, pageable)).isEqualTo(page);
  }

  @Test
  void createActivityAnswers201WithTheLocationAndTheActivity() {
    when(activityService.create(3L, REQUEST)).thenReturn(ACTIVITY);

    ResponseEntity<ActivityDTO> response = controller.createActivity(3L, REQUEST);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getHeaders().getLocation()).hasToString("/api/v1/trips/3/activities/5");
    assertThat(response.getBody()).isEqualTo(ACTIVITY);
  }

  @Test
  void getActivityReturnsTheActivityOfTheService() {
    when(activityService.findById(3L, 5L)).thenReturn(ACTIVITY);

    assertThat(controller.getActivity(3L, 5L)).isEqualTo(ACTIVITY);
  }

  @Test
  void updateActivityReturnsTheUpdatedActivity() {
    when(activityService.update(3L, 5L, REQUEST)).thenReturn(ACTIVITY);

    assertThat(controller.updateActivity(3L, 5L, REQUEST)).isEqualTo(ACTIVITY);
  }

  @Test
  void deleteActivityAnswers204() {
    ResponseEntity<Void> response = controller.deleteActivity(3L, 5L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(activityService).delete(3L, 5L);
  }

  @Test
  void theFirstImageAnswers201WithItsLocation() {
    when(activityService.replaceImage(3L, 5L, PICTURE)).thenReturn(new ImageResult(IMAGE, true));

    ResponseEntity<ImageDTO> response = controller.replaceActivityImage(3L, 5L, PICTURE);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getHeaders().getLocation())
        .hasToString("/api/v1/trips/3/activities/5/image");
    assertThat(response.getBody()).isEqualTo(IMAGE);
  }

  @Test
  void aReplacedImageAnswers200WithoutLocation() {
    when(activityService.replaceImage(3L, 5L, PICTURE)).thenReturn(new ImageResult(IMAGE, false));

    ResponseEntity<ImageDTO> response = controller.replaceActivityImage(3L, 5L, PICTURE);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getLocation()).isNull();
    assertThat(response.getBody()).isEqualTo(IMAGE);
  }

  @Test
  void getImageAnswersTheBytesWithTheirTypeAndNoCache() {
    byte[] data = ImageTestData.png(50);
    when(activityService.findImageContent(3L, 5L))
        .thenReturn(new ImageContentDTO(data, "image/png"));

    ResponseEntity<byte[]> response = controller.getActivityImage(3L, 5L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
    assertThat(response.getHeaders().getContentLength()).isEqualTo(50);
    assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-cache");
    assertThat(response.getBody()).isEqualTo(data);
  }

  @Test
  void deleteImageAnswers204() {
    ResponseEntity<Void> response = controller.deleteActivityImage(3L, 5L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(activityService).deleteImage(3L, 5L);
  }
}
