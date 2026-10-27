package com.ruben.backend.activity.controller;

import com.ruben.backend.activity.dto.TripConflictsDTO;
import com.ruben.backend.activity.service.ConflictService;
import com.ruben.backend.common.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/conflicts")
public class ConflictController {

  private final ConflictService conflictService;

  public ConflictController(ConflictService conflictService) {
    this.conflictService = conflictService;
  }

  @Operation(
      summary = "List a trip's itinerary conflicts",
      description =
          "Only the trip's creator. Warnings, never errors: `overlaps` are pairs of activities of"
              + " the same day whose times intersect (touching times do not; activities without"
              + " start and end time are ignored), `outsideDates` are activities before the trip"
              + " starts or after it ends, and `budget` is set only when the total cost is above"
              + " the trip's budget (null otherwise). Not paginated: it is one small report.")
  @ApiResponse(responseCode = "200", description = "The conflicts, all empty when there are none")
  @ApiResponse(
      responseCode = "400",
      description = "The trip id is not a number",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Not logged in",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = "The trip belongs to another user",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "No trip has that id",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping
  public TripConflictsDTO getConflicts(@PathVariable Long tripId) {
    return conflictService.findConflicts(tripId);
  }
}
