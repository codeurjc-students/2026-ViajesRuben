package com.ruben.backend.trip.controller;

import com.ruben.backend.common.error.ErrorResponse;
import com.ruben.backend.trip.dto.TripDTO;
import com.ruben.backend.trip.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
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

  @Operation(summary = "List trips")
  @ApiResponse(responseCode = "200", description = "The trips")
  @GetMapping
  public List<TripDTO> getTrips() {
    return tripService.findAll();
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
