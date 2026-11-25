package com.ruben.backend.metrics.controller;

import com.ruben.backend.common.error.ErrorResponse;
import com.ruben.backend.metrics.dto.PlatformMetricsDTO;
import com.ruben.backend.metrics.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Platform metrics for the administrator; `/api/v1/admin/**` is restricted to the ADMIN role. */
@RestController
@RequestMapping("/api/v1/admin/metrics")
public class AdminMetricsController {

  private final MetricsService metricsService;

  public AdminMetricsController(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  @Operation(
      summary = "Get the platform totals",
      description =
          "Administrator only. The total number of users (regular accounts, blocked ones included,"
              + " the administrator not counted), of trips (published or not) and of activities."
              + " Not paginated: it is three numbers.")
  @ApiResponse(responseCode = "200", description = "The platform totals")
  @ApiResponse(
      responseCode = "401",
      description = "Not logged in",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = "The caller is not the administrator",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping
  public PlatformMetricsDTO getMetrics() {
    return metricsService.getPlatformMetrics();
  }
}
