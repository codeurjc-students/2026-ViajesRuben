package com.ruben.backend.trip.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.image.dto.ImageContentDTO;
import com.ruben.backend.image.dto.ImageDTO;
import com.ruben.backend.image.dto.ImageResult;
import com.ruben.backend.trip.dto.TripDTO;
import com.ruben.backend.trip.dto.TripRequest;
import com.ruben.backend.trip.service.TripService;
import java.math.BigDecimal;
import java.time.LocalDate;
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

@Tag("B4")
@ExtendWith(MockitoExtension.class)
class TripControllerUnitTest {

  private static final TripDTO TRIP =
      new TripDTO(
          3L,
          7L,
          "Rome",
          "Rome",
          LocalDate.of(2027, 7, 10),
          LocalDate.of(2027, 7, 17),
          new BigDecimal("300.00"),
          null,
          null);
  private static final TripRequest REQUEST =
      new TripRequest(
          "Rome", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17), null, null);
  private static final MockMultipartFile PICTURE =
      new MockMultipartFile("file", "a.png", "image/png", ImageTestData.png(100));
  private static final ImageDTO COVER = new ImageDTO(UUID.randomUUID(), "image/png", 100);

  @Mock private TripService tripService;
  @InjectMocks private TripController controller;

  @Test
  void getTripsReturnsThePageOfTheService() {
    Pageable pageable = PageRequest.of(0, 10);
    PageDTO<TripDTO> page = new PageDTO<>(List.of(TRIP), 0, 10, 1, true);
    when(tripService.findPage(pageable)).thenReturn(page);

    assertThat(controller.getTrips(pageable)).isEqualTo(page);
  }

  @Test
  void createTripAnswers201WithTheLocationAndTheTrip() {
    when(tripService.create(REQUEST)).thenReturn(TRIP);

    ResponseEntity<TripDTO> response = controller.createTrip(REQUEST);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getHeaders().getLocation()).hasToString("/api/v1/trips/3");
    assertThat(response.getBody()).isEqualTo(TRIP);
  }

  @Test
  void getTripReturnsTheTripOfTheService() {
    when(tripService.findById(3L)).thenReturn(TRIP);

    assertThat(controller.getTrip(3L)).isEqualTo(TRIP);
  }

  @Test
  void updateTripReturnsTheUpdatedTrip() {
    when(tripService.update(3L, REQUEST)).thenReturn(TRIP);

    assertThat(controller.updateTrip(3L, REQUEST)).isEqualTo(TRIP);
  }

  @Test
  void deleteTripAnswers204() {
    ResponseEntity<Void> response = controller.deleteTrip(3L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(tripService).delete(3L);
  }

  @Test
  void theFirstCoverAnswers201WithItsLocation() {
    when(tripService.replaceCover(3L, PICTURE)).thenReturn(new ImageResult(COVER, true));

    ResponseEntity<ImageDTO> response = controller.replaceCover(3L, PICTURE);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getHeaders().getLocation()).hasToString("/api/v1/trips/3/cover");
    assertThat(response.getBody()).isEqualTo(COVER);
  }

  @Test
  void aReplacedCoverAnswers200WithoutLocation() {
    when(tripService.replaceCover(3L, PICTURE)).thenReturn(new ImageResult(COVER, false));

    ResponseEntity<ImageDTO> response = controller.replaceCover(3L, PICTURE);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getLocation()).isNull();
    assertThat(response.getBody()).isEqualTo(COVER);
  }

  @Test
  void getCoverAnswersTheBytesWithTheirTypeAndNoCache() {
    byte[] data = ImageTestData.png(50);
    when(tripService.findCoverContent(3L)).thenReturn(new ImageContentDTO(data, "image/png"));

    ResponseEntity<byte[]> response = controller.getCover(3L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
    assertThat(response.getHeaders().getContentLength()).isEqualTo(50);
    assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-cache");
    assertThat(response.getBody()).isEqualTo(data);
  }

  @Test
  void deleteCoverAnswers204() {
    ResponseEntity<Void> response = controller.deleteCover(3L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(tripService).deleteCover(3L);
  }
}
