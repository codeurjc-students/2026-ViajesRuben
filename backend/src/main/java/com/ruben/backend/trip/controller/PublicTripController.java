package com.ruben.backend.trip.controller;

import com.ruben.backend.activity.dto.ActivityDTO;
import com.ruben.backend.activity.service.ActivityService;
import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.ErrorResponse;
import com.ruben.backend.trip.dto.PublicTripDTO;
import com.ruben.backend.trip.dto.PublicTripSummaryDTO;
import com.ruben.backend.trip.service.PublicTripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.time.LocalDate;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The public, read-only itineraries of the home page: the trips their creators published. */
@RestController
@RequestMapping("/api/v1/trips/public")
public class PublicTripController {

  private static final String NO_PUBLIC_TRIP = "No published trip has that id";

  private final PublicTripService publicTripService;
  private final ActivityService activityService;

  public PublicTripController(
      PublicTripService publicTripService, ActivityService activityService) {
    this.publicTripService = publicTripService;
    this.activityService = activityService;
  }

  @Operation(
      summary = "List the public trips",
      description =
          "Public, no login. The published trips of every user, each with the first activity of its"
              + " first three days (`firstDays`; days without activities are left out) and the"
              + " author's name, never an email. Paginated: `?page=0&size=10` (50 at most), the"
              + " most recently published first by default; `sort` accepts `id`, `name`,"
              + " `destination`, `startDate`, `endDate`, `createdAt` and `publishedAt`."
              + " `search` keeps the trips whose name or destination contains the text, ignoring"
              + " case; blank lists all.")
  @ApiResponse(responseCode = "200", description = "One page of public trips")
  @ApiResponse(
      responseCode = "400",
      description = "Invalid page, size or sort parameter",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping
  public PageDTO<PublicTripSummaryDTO> getPublicTrips(
      @Parameter(description = "Text to find in the trip name or destination, ignoring case")
          @RequestParam(required = false)
          String search,
      @ParameterObject
          @PageableDefault(
              size = 10,
              sort = {"publishedAt", "id"},
              direction = Sort.Direction.DESC)
          Pageable pageable) {
    return publicTripService.findPage(search, pageable);
  }

  @Operation(
      summary = "Get a public trip by id",
      description =
          "Public, no login. Only published trips; an unpublished or unknown id is 404, never 403.")
  @ApiResponse(responseCode = "200", description = "The public trip")
  @ApiResponse(
      responseCode = "400",
      description = "The id is not a number",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = NO_PUBLIC_TRIP,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping("/{id}")
  public PublicTripDTO getPublicTrip(@PathVariable Long id) {
    return publicTripService.findById(id);
  }

  @Operation(
      summary = "List a public trip's activities",
      description =
          "Public, no login. Paginated like `GET /api/v1/trips/{tripId}/activities`:"
              + " `?page=0&size=10`, sorted by date and start time by default, `date=yyyy-MM-dd`"
              + " keeps only the activities of that day.")
  @ApiResponse(responseCode = "200", description = "One page of activities")
  @ApiResponse(
      responseCode = "400",
      description = "Invalid trip id, date, page, size or sort parameter",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = NO_PUBLIC_TRIP,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping("/{id}/activities")
  public PageDTO<ActivityDTO> getPublicTripActivities(
      @PathVariable Long id,
      @Parameter(description = "Only the activities of this day, `yyyy-MM-dd`")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate date,
      @ParameterObject
          @PageableDefault(
              size = 10,
              sort = {"date", "startTime", "id"})
          Pageable pageable) {
    return activityService.findPublicPage(id, date, pageable);
  }
}
