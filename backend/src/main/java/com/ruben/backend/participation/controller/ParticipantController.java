package com.ruben.backend.participation.controller;

import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.ErrorResponse;
import com.ruben.backend.participation.dto.ParticipantDTO;
import com.ruben.backend.participation.service.ParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/participants")
public class ParticipantController {

  private static final String NOT_LOGGED_IN = "Not logged in";

  private final ParticipantService participantService;

  public ParticipantController(ParticipantService participantService) {
    this.participantService = participantService;
  }

  @Operation(
      summary = "List the participants of a trip",
      description =
          "Anyone who can see the trip: its creator, its participants and the administrator. The"
              + " creator is not listed, only the people who joined. Each one has `userId`, `name`,"
              + " `avatarId` (null without avatar; the picture is at `GET /api/v1/images/{avatarId}`)"
              + " and `joinedAt`. The `email` is only returned to the trip's creator and is null for"
              + " everyone else. Paginated: `?page=0&size=10` (50 at most), oldest first by default."
              + " `sort` accepts `id` and `joinedAt`.")
  @ApiResponse(responseCode = "200", description = "One page of participants")
  @ApiResponse(
      responseCode = "400",
      description = "Invalid trip id, page, size or sort parameter",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
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
  public PageDTO<ParticipantDTO> getParticipants(
      @PathVariable Long tripId,
      @ParameterObject
          @PageableDefault(
              size = 10,
              sort = {"joinedAt", "id"},
              direction = Sort.Direction.ASC)
          Pageable pageable) {
    return participantService.findPage(tripId, pageable);
  }

  @Operation(
      summary = "Remove a participant from a trip, or leave it",
      description =
          "The trip's creator removes any participant; a participant can only remove themselves,"
              + " which is leaving the trip. The change is immediate: the removed user's next"
              + " request to the trip is 403. Their invitation stays as history and they can be"
              + " invited again. The administrator does not moderate participants.")
  @ApiResponse(responseCode = "204", description = "The participant is no longer in the trip")
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
      description =
          "A participant removing someone else, the administrator, or anyone who is not in the"
              + " trip",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "No trip has that id, or that user is not one of its participants",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "409",
      description = "The user is the trip's creator, who cannot leave their own trip",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @DeleteMapping("/{userId}")
  public ResponseEntity<Void> removeParticipant(
      @PathVariable Long tripId, @PathVariable Long userId) {
    participantService.remove(tripId, userId);
    return ResponseEntity.noContent().build();
  }
}
