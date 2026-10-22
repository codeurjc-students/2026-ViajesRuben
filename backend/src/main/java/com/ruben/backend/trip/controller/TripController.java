package com.ruben.backend.trip.controller;

import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.ErrorResponse;
import com.ruben.backend.trip.dto.TripDTO;
import com.ruben.backend.trip.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips")
public class TripController {

  private final TripService tripService;

  public TripController(TripService tripService) {
    this.tripService = tripService;
  }

  @Operation(
      summary = "List trips",
      description =
          "Paginated: `?page=0&size=10` (10 per page by default, 50 at most). Sorted by start date.")
  @ApiResponse(responseCode = "200", description = "One page of trips")
  @ApiResponse(
      responseCode = "400",
      description = "Invalid page, size or sort parameter",
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

  @Operation(summary = "Get a trip by id")
  @ApiResponse(responseCode = "200", description = "The trip")
  @ApiResponse(
      responseCode = "400",
      description = "The id is not a number",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Not logged in",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "No trip has that id",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping("/{id}")
  public TripDTO getTrip(@PathVariable Long id) {
    return tripService.findById(id);
  }
}
