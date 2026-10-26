package com.ruben.backend.trip.controller;

import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.ErrorResponse;
import com.ruben.backend.image.dto.ImageContentDTO;
import com.ruben.backend.image.dto.ImageDTO;
import com.ruben.backend.image.dto.ImageResult;
import com.ruben.backend.trip.dto.TripDTO;
import com.ruben.backend.trip.dto.TripRequest;
import com.ruben.backend.trip.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/trips")
public class TripController {

  private static final String NOT_LOGGED_IN = "Not logged in";
  private static final String NOT_CREATOR = "The trip belongs to another user";
  private static final String NO_TRIP = "No trip has that id";

  private final TripService tripService;

  public TripController(TripService tripService) {
    this.tripService = tripService;
  }

  @Operation(
      summary = "List one's own trips",
      description =
          "Only the trips the logged-in user created. Paginated: `?page=0&size=10` (10 per page by"
              + " default, 50 at most). Sorted by start date by default; `sort` accepts `id`,"
              + " `name`, `destination`, `startDate`, `endDate` and `createdAt`.")
  @ApiResponse(responseCode = "200", description = "One page of trips")
  @ApiResponse(
      responseCode = "400",
      description = "Invalid page, size or sort parameter",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping
  public PageDTO<TripDTO> getTrips(
      @ParameterObject
          @PageableDefault(
              size = 10,
              sort = {"startDate", "id"})
          Pageable pageable) {
    return tripService.findPage(pageable);
  }

  @Operation(
      summary = "Create a trip",
      description =
          "The logged-in user becomes the creator. Dates are `yyyy-MM-dd` and the end must not be"
              + " before the start. `budget` and `description` are optional. The cover is set"
              + " afterwards with `PUT /api/v1/trips/{id}/cover`. The `Location` header is the URL"
              + " of the new trip.")
  @ApiResponse(responseCode = "201", description = "The trip was created")
  @ApiResponse(
      responseCode = "400",
      description = "Missing or invalid fields, or the end date is before the start date",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PostMapping
  public ResponseEntity<TripDTO> createTrip(@Valid @RequestBody TripRequest request) {
    TripDTO trip = tripService.create(request);
    return ResponseEntity.created(URI.create("/api/v1/trips/" + trip.id())).body(trip);
  }

  @Operation(
      summary = "Get a trip by id",
      description =
          "Only the creator. `coverImageId` is null without cover; otherwise the picture is at"
              + " `GET /api/v1/images/{coverImageId}`.")
  @ApiResponse(responseCode = "200", description = "The trip")
  @ApiResponse(
      responseCode = "400",
      description = "The id is not a number",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = NOT_CREATOR,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = NO_TRIP,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping("/{id}")
  public TripDTO getTrip(@PathVariable Long id) {
    return tripService.findById(id);
  }

  @Operation(
      summary = "Replace a trip's details",
      description =
          "Only the creator. Every field is replaced; the cover is not changed. The new dates"
              + " cannot leave an activity of the trip outside them (409): move or delete it first.")
  @ApiResponse(responseCode = "200", description = "The updated trip")
  @ApiResponse(
      responseCode = "400",
      description = "Missing or invalid fields, or the end date is before the start date",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = NOT_CREATOR,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = NO_TRIP,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "409",
      description = "Some activities of the trip fall outside the new dates",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PutMapping("/{id}")
  public TripDTO updateTrip(@PathVariable Long id, @Valid @RequestBody TripRequest request) {
    return tripService.update(id, request);
  }

  @Operation(
      summary = "Delete a trip",
      description = "Only the creator. The cover image is deleted with the trip.")
  @ApiResponse(responseCode = "204", description = "The trip was deleted")
  @ApiResponse(
      responseCode = "400",
      description = "The id is not a number",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = NOT_CREATOR,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = NO_TRIP,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteTrip(@PathVariable Long id) {
    tripService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Upload or replace a trip's cover",
      description =
          "Only the creator. Multipart form with the picture in the `file` part: JPEG, PNG or WebP"
              + " up to the configured size. The previous cover is deleted.")
  @ApiResponse(
      responseCode = "201",
      description = "The trip had no cover; `Location` is the cover URL")
  @ApiResponse(responseCode = "200", description = "The previous cover was replaced")
  @ApiResponse(
      responseCode = "400",
      description = "Missing, empty or not a JPEG, PNG or WebP image",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = NOT_CREATOR,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = NO_TRIP,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "413",
      description = "The image is too large",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PutMapping(path = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ImageDTO> replaceCover(
      @PathVariable Long id, @RequestPart("file") MultipartFile file) {
    ImageResult result = tripService.replaceCover(id, file);
    if (result.created()) {
      return ResponseEntity.created(URI.create("/api/v1/trips/" + id + "/cover"))
          .body(result.image());
    }
    return ResponseEntity.ok(result.image());
  }

  @Operation(
      summary = "Get the bytes of a trip's cover",
      description =
          "Only the creator. Not cached, because it changes when the cover is replaced; the SPA"
              + " uses `GET /api/v1/images/{coverImageId}` instead.")
  @ApiResponse(
      responseCode = "200",
      description = "The cover, with its `Content-Type`",
      content = @Content(mediaType = "image/*"))
  @ApiResponse(
      responseCode = "400",
      description = "The id is not a number",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = NOT_CREATOR,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "No trip has that id, or the trip has no cover",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping("/{id}/cover")
  public ResponseEntity<byte[]> getCover(@PathVariable Long id) {
    ImageContentDTO cover = tripService.findCoverContent(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(cover.contentType()))
        .contentLength(cover.data().length)
        .cacheControl(CacheControl.noCache())
        .body(cover.data());
  }

  @Operation(
      summary = "Delete a trip's cover",
      description = "Only the creator. Also answers 204 when there was no cover.")
  @ApiResponse(responseCode = "204", description = "The trip has no cover now")
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = NOT_CREATOR,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = NO_TRIP,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @DeleteMapping("/{id}/cover")
  public ResponseEntity<Void> deleteCover(@PathVariable Long id) {
    tripService.deleteCover(id);
    return ResponseEntity.noContent().build();
  }
}
