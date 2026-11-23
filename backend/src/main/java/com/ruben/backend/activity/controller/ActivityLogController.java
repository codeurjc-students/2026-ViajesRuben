package com.ruben.backend.activity.controller;

import com.ruben.backend.activity.dto.ActivityLogEntryDTO;
import com.ruben.backend.activity.service.ActivityLogService;
import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/activity-log")
public class ActivityLogController {

  private final ActivityLogService activityLogService;

  public ActivityLogController(ActivityLogService activityLogService) {
    this.activityLogService = activityLogService;
  }

  @Operation(
      summary = "List the changes made to a trip's itinerary",
      description =
          "The change log of the trip: who added, edited or deleted which activity, and when. Anyone"
              + " who can see the trip may read it: its creator, its participants and the"
              + " administrator. Each line has `action` (`CREATED`, `UPDATED` or `DELETED`),"
              + " `activityName` and `userName` as they were at the time of the change,"
              + " `activityId` (null once the activity is deleted), `userId`, `createdAt` and, for an"
              + " edit, `changedFields`: the names of the fields that changed (`name`, `description`,"
              + " `location`, `date`, `startTime`, `endTime`, `cost`, `category`, `image`), never"
              + " their values. Paginated: `?page=0&size=10` (50 at most), newest first by default."
              + " `sort` accepts `id` and `createdAt`.")
  @ApiResponse(responseCode = "200", description = "One page of the change log")
  @ApiResponse(
      responseCode = "400",
      description = "Invalid trip id, page, size or sort parameter",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Not logged in",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = "The user may not see the trip",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "No trip has that id",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping
  public PageDTO<ActivityLogEntryDTO> getActivityLog(
      @PathVariable Long tripId,
      @ParameterObject
          @PageableDefault(
              size = 10,
              sort = {"createdAt", "id"},
              direction = Sort.Direction.DESC)
          Pageable pageable) {
    return activityLogService.findPage(tripId, pageable);
  }
}
