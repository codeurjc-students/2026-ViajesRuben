package com.ruben.backend.invitation.controller;

import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.ErrorResponse;
import com.ruben.backend.invitation.dto.InvitationDTO;
import com.ruben.backend.invitation.dto.InvitationRequest;
import com.ruben.backend.invitation.service.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/invitations")
public class InvitationController {

  private static final String NOT_LOGGED_IN = "Not logged in";
  private static final String NOT_ALLOWED = "Only the trip's creator, not even the administrator";

  private final InvitationService invitationService;

  public InvitationController(InvitationService invitationService) {
    this.invitationService = invitationService;
  }

  @Operation(
      summary = "Invite someone to a trip",
      description =
          "Only the trip's creator. The email may have no account yet; it is trimmed and lower"
              + " cased. A pending invitation is created and an email with a link to answer it is"
              + " sent to that address. The token of the link is never returned. The `Location`"
              + " header is the URL of the new invitation.")
  @ApiResponse(responseCode = "201", description = "The invitation was created")
  @ApiResponse(
      responseCode = "400",
      description = "Missing or invalid email, or it is the creator's own",
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
  @ApiResponse(
      responseCode = "409",
      description = "The email already has a pending invitation to this trip",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PostMapping
  public ResponseEntity<InvitationDTO> createInvitation(
      @PathVariable Long tripId, @Valid @RequestBody InvitationRequest request) {
    InvitationDTO invitation = invitationService.create(tripId, request);
    return ResponseEntity.created(
            URI.create("/api/v1/trips/" + tripId + "/invitations/" + invitation.id()))
        .body(invitation);
  }

  @Operation(
      summary = "List a trip's invitations",
      description =
          "Only the trip's creator. Paginated: `?page=0&size=10` (50 at most), newest first by"
              + " default. `sort` accepts `id`, `invitedEmail`, `status` and `sentAt`.")
  @ApiResponse(responseCode = "200", description = "One page of invitations")
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
      description = NOT_ALLOWED,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "No trip has that id",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping
  public PageDTO<InvitationDTO> getInvitations(
      @PathVariable Long tripId,
      @ParameterObject
          @PageableDefault(
              size = 10,
              sort = {"sentAt", "id"},
              direction = Sort.Direction.DESC)
          Pageable pageable) {
    return invitationService.findPage(tripId, pageable);
  }

  @Operation(summary = "Get an invitation by id", description = "Only the trip's creator.")
  @ApiResponse(responseCode = "200", description = "The invitation")
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
      description = "No trip has that id, or it has no such invitation",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping("/{invitationId}")
  public InvitationDTO getInvitation(@PathVariable Long tripId, @PathVariable Long invitationId) {
    return invitationService.findById(tripId, invitationId);
  }
}
