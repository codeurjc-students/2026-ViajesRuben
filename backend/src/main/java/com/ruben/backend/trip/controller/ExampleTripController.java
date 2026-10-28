package com.ruben.backend.trip.controller;

import com.ruben.backend.activity.dto.ActivityDTO;
import com.ruben.backend.activity.service.ActivityService;
import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.ErrorResponse;
import com.ruben.backend.trip.dto.ExampleTripDTO;
import com.ruben.backend.trip.dto.TripDTO;
import com.ruben.backend.trip.service.ExampleTripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.time.LocalDate;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The public, read-only example trips of the home page: the trips the administrator created. */
@RestController
@RequestMapping("/api/v1/trips/examples")
public class ExampleTripController {

  private static final String NO_EXAMPLE = "No example trip has that id";

  private final ExampleTripService exampleTripService;
  private final ActivityService activityService;

  public ExampleTripController(
      ExampleTripService exampleTripService, ActivityService activityService) {
    this.exampleTripService = exampleTripService;
    this.activityService = activityService;
  }

  @Operation(
      summary = "List the example trips",
      description =
          "Public, no login. The trips the administrator created, each with the first activity of"
              + " its first three days (`firstDays`; days without activities are left out)."
              + " Paginated: `?page=0&size=10` (50 at most), sorted by start date by default;"
              + " `sort` accepts `id`, `name`, `destination`, `startDate`, `endDate` and"
              + " `createdAt`.")
  @ApiResponse(responseCode = "200", description = "One page of example trips")
  @ApiResponse(
      responseCode = "400",
      description = "Invalid page, size or sort parameter",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping
  public PageDTO<ExampleTripDTO> getExamples(
      @ParameterObject
          @PageableDefault(
              size = 10,
              sort = {"startDate", "id"})
          Pageable pageable) {
    return exampleTripService.findPage(pageable);
  }

  @Operation(
      summary = "Get an example trip by id",
      description = "Public, no login. Only trips the administrator created; any other id is 404.")
  @ApiResponse(responseCode = "200", description = "The example trip")
  @ApiResponse(
      responseCode = "400",
      description = "The id is not a number",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = NO_EXAMPLE,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping("/{id}")
  public TripDTO getExample(@PathVariable Long id) {
    return exampleTripService.findById(id);
  }

  @Operation(
      summary = "List an example trip's activities",
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
      description = NO_EXAMPLE,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping("/{id}/activities")
  public PageDTO<ActivityDTO> getExampleActivities(
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
    return activityService.findExamplePage(id, date, pageable);
  }
}
