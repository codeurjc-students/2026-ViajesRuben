package com.ruben.backend.activity.controller;

import com.ruben.backend.activity.dto.ActivityDTO;
import com.ruben.backend.activity.dto.ActivityRequest;
import com.ruben.backend.activity.dto.TripCostDTO;
import com.ruben.backend.activity.service.ActivityService;
import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.ErrorResponse;
import com.ruben.backend.image.dto.ImageContentDTO;
import com.ruben.backend.image.dto.ImageDTO;
import com.ruben.backend.image.dto.ImageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/activities")
public class ActivityController {

  private static final String NOT_LOGGED_IN = "Not logged in";
  private static final String NOT_ALLOWED = "The trip belongs to another user";
  private static final String NO_ACTIVITY = "No trip has that id, or it has no such activity";

  private final ActivityService activityService;

  public ActivityController(ActivityService activityService) {
    this.activityService = activityService;
  }

  private static String activityUrl(Long tripId, Long id) {
    return "/api/v1/trips/" + tripId + "/activities/" + id;
  }

  @Operation(
      summary = "List a trip's activities",
      description =
          "Only the trip's creator. Paginated: `?page=0&size=10` (50 at most), sorted by date and"
              + " start time by default; activities without a start time come first on their"
              + " day. `date=yyyy-MM-dd` keeps only the activities of that day. `sort` accepts"
              + " `id`, `date`, `startTime`, `name`, `cost` and `createdAt`.")
  @ApiResponse(responseCode = "200", description = "One page of activities")
  @ApiResponse(
      responseCode = "400",
      description = "Invalid trip id, date, page, size or sort parameter",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = NOT_ALLOWED,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "No trip has that id",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping
  public PageDTO<ActivityDTO> getActivities(
      @PathVariable Long tripId,
      @Parameter(description = "Only the activities of this day, `yyyy-MM-dd`")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate date,
      @ParameterObject
          @PageableDefault(
              size = 10,
              sort = {"date", "startTime", "id"})
          Pageable pageable) {
    return activityService.findPage(tripId, date, pageable);
  }

  @Operation(
      summary = "Get what a trip's activities cost",
      description =
          "Only the trip's creator. `totalCost` is the sum of the costs of all the activities (0"
              + " without any cost) and `remainingBudget` is the trip's budget minus that total,"
              + " negative when the activities cost more and null when the trip has no budget.")
  @ApiResponse(responseCode = "200", description = "The total cost and the remaining budget")
  @ApiResponse(
      responseCode = "400",
      description = "The trip id is not a number",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = NOT_ALLOWED,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "No trip has that id",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping("/cost")
  public TripCostDTO getCost(@PathVariable Long tripId) {
    return activityService.findCost(tripId);
  }

  @Operation(
      summary = "Create an activity",
      description =
          "Only the trip's creator, who becomes the author. `date` must be within the trip's dates;"
              + " times are `HH:mm`, the end strictly after the start and not without a start."
              + " The picture is set afterwards with `PUT .../{activityId}/image`. The `Location`"
              + " header is the URL of the new activity.")
  @ApiResponse(responseCode = "201", description = "The activity was created")
  @ApiResponse(
      responseCode = "400",
      description = "Missing or invalid fields, or the date is outside the trip's dates",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = NOT_ALLOWED,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "No trip has that id",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PostMapping
  public ResponseEntity<ActivityDTO> createActivity(
      @PathVariable Long tripId, @Valid @RequestBody ActivityRequest request) {
    ActivityDTO activity = activityService.create(tripId, request);
    return ResponseEntity.created(URI.create(activityUrl(tripId, activity.id()))).body(activity);
  }

  @Operation(
      summary = "Get an activity by id",
      description =
          "Only the trip's creator. `imageId` is null without picture; otherwise the picture is at"
              + " `GET /api/v1/images/{imageId}`.")
  @ApiResponse(responseCode = "200", description = "The activity")
  @ApiResponse(
      responseCode = "400",
      description = "An id is not a number",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = NOT_ALLOWED,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = NO_ACTIVITY,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping("/{activityId}")
  public ActivityDTO getActivity(@PathVariable Long tripId, @PathVariable Long activityId) {
    return activityService.findById(tripId, activityId);
  }

  @Operation(
      summary = "Replace an activity's details",
      description = "Only the trip's creator. Every field is replaced; the picture is not changed.")
  @ApiResponse(responseCode = "200", description = "The updated activity")
  @ApiResponse(
      responseCode = "400",
      description = "Missing or invalid fields, or the date is outside the trip's dates",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = NOT_ALLOWED,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = NO_ACTIVITY,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PutMapping("/{activityId}")
  public ActivityDTO updateActivity(
      @PathVariable Long tripId,
      @PathVariable Long activityId,
      @Valid @RequestBody ActivityRequest request) {
    return activityService.update(tripId, activityId, request);
  }

  @Operation(
      summary = "Delete an activity",
      description = "Only the trip's creator. The picture is deleted with the activity.")
  @ApiResponse(responseCode = "204", description = "The activity was deleted")
  @ApiResponse(
      responseCode = "400",
      description = "An id is not a number",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = NOT_ALLOWED,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = NO_ACTIVITY,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @DeleteMapping("/{activityId}")
  public ResponseEntity<Void> deleteActivity(
      @PathVariable Long tripId, @PathVariable Long activityId) {
    activityService.delete(tripId, activityId);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Upload or replace an activity's picture",
      description =
          "Only the trip's creator. Multipart form with the picture in the `file` part: JPEG, PNG"
              + " or WebP up to the configured size. The previous picture is deleted.")
  @ApiResponse(
      responseCode = "201",
      description = "The activity had no picture; `Location` is the picture URL")
  @ApiResponse(responseCode = "200", description = "The previous picture was replaced")
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
      description = NOT_ALLOWED,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = NO_ACTIVITY,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "413",
      description = "The image is too large",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PutMapping(path = "/{activityId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ImageDTO> replaceActivityImage(
      @PathVariable Long tripId,
      @PathVariable Long activityId,
      @RequestPart("file") MultipartFile file) {
    ImageResult result = activityService.replaceImage(tripId, activityId, file);
    if (result.created()) {
      return ResponseEntity.created(URI.create(activityUrl(tripId, activityId) + "/image"))
          .body(result.image());
    }
    return ResponseEntity.ok(result.image());
  }

  @Operation(
      summary = "Get the bytes of an activity's picture",
      description =
          "Only the trip's creator. Not cached, because it changes when the picture is replaced;"
              + " the SPA uses `GET /api/v1/images/{imageId}` instead.")
  @ApiResponse(
      responseCode = "200",
      description = "The picture, with its `Content-Type`",
      content = @Content(mediaType = "image/*"))
  @ApiResponse(
      responseCode = "400",
      description = "An id is not a number",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = NOT_ALLOWED,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "No such trip or activity, or the activity has no picture",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping("/{activityId}/image")
  public ResponseEntity<byte[]> getActivityImage(
      @PathVariable Long tripId, @PathVariable Long activityId) {
    ImageContentDTO image = activityService.findImageContent(tripId, activityId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(image.contentType()))
        .contentLength(image.data().length)
        .cacheControl(CacheControl.noCache())
        .body(image.data());
  }

  @Operation(
      summary = "Delete an activity's picture",
      description = "Only the trip's creator. Also answers 204 when there was no picture.")
  @ApiResponse(responseCode = "204", description = "The activity has no picture now")
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = NOT_ALLOWED,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = NO_ACTIVITY,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @DeleteMapping("/{activityId}/image")
  public ResponseEntity<Void> deleteActivityImage(
      @PathVariable Long tripId, @PathVariable Long activityId) {
    activityService.deleteImage(tripId, activityId);
    return ResponseEntity.noContent().build();
  }
}
