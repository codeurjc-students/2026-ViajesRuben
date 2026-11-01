package com.ruben.backend.trip.controller;

import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.ErrorResponse;
import com.ruben.backend.trip.dto.AdminTripDTO;
import com.ruben.backend.trip.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trip moderation for the administrator; `/api/v1/admin/**` is restricted to the ADMIN role. A trip
 * is read with `GET /api/v1/trips/{id}` and deleted with `DELETE /api/v1/trips/{id}`, which accept
 * the administrator for any trip.
 */
@RestController
@RequestMapping("/api/v1/admin/trips")
public class AdminTripController {

  private final TripService tripService;

  public AdminTripController(TripService tripService) {
    this.tripService = tripService;
  }

  @Operation(
      summary = "List and search all the trips",
      description =
          "Administrator only. The trips of every user. `search` keeps the trips whose name,"
              + " destination or creator's email contains the text, ignoring case. Paginated:"
              + " `?page=0&size=10` (10 per page by default, 50 at most). Newest first by default;"
              + " `sort` accepts `id`, `name`, `destination`, `startDate`, `endDate` and"
              + " `createdAt`.")
  @ApiResponse(responseCode = "200", description = "One page of trips")
  @ApiResponse(
      responseCode = "400",
      description = "The `sort` parameter names a property that is not sortable",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Not logged in",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = "The caller is not the administrator",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping
  public PageDTO<AdminTripDTO> getTrips(
      @RequestParam(required = false) String search,
      @ParameterObject
          @PageableDefault(
              size = 10,
              sort = {"createdAt", "id"},
              direction = Sort.Direction.DESC)
          Pageable pageable) {
    return tripService.findAllPage(search, pageable);
  }
}
