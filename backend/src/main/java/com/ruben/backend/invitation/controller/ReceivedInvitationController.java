package com.ruben.backend.invitation.controller;

import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.ErrorResponse;
import com.ruben.backend.invitation.dto.InvitationResponseRequest;
import com.ruben.backend.invitation.dto.ReceivedInvitationDTO;
import com.ruben.backend.invitation.service.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The invitations a user has received: see them, open one from an email link and answer it. */
@RestController
@RequestMapping("/api/v1/invitations")
public class ReceivedInvitationController {

  private static final String NOT_LOGGED_IN = "Not logged in";
  private static final String NOT_ADDRESSEE = "The invitation was sent to another email";

  private final InvitationService invitationService;

  public ReceivedInvitationController(InvitationService invitationService) {
    this.invitationService = invitationService;
  }

  @Operation(
      summary = "List my pending invitations",
      description =
          "The pending invitations addressed to the email of the logged-in user. Paginated:"
              + " `?page=0&size=10` (50 at most), newest first by default. `sort` accepts `id` and"
              + " `sentAt`. Answered invitations are not listed.")
  @ApiResponse(responseCode = "200", description = "One page of invitations")
  @ApiResponse(
      responseCode = "400",
      description = "Invalid page, size or sort parameter",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping
  public PageDTO<ReceivedInvitationDTO> getMyInvitations(
      @ParameterObject
          @PageableDefault(
              size = 10,
              sort = {"sentAt", "id"},
              direction = Sort.Direction.DESC)
          Pageable pageable) {
    return invitationService.findMine(pageable);
  }

  @Operation(
      summary = "Get an invitation by the token of its email link",
      description =
          "Only for the account the invitation was sent to; the answer shows the trip, who invites"
              + " and the status, whatever it is. Invitations never expire.")
  @ApiResponse(responseCode = "200", description = "The invitation")
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = NOT_ADDRESSEE,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "No invitation has that token, or its trip was deleted",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping("/token/{token}")
  public ReceivedInvitationDTO getInvitationByToken(@PathVariable String token) {
    return invitationService.findByToken(token);
  }

  @Operation(
      summary = "Accept or reject an invitation",
      description =
          "Only the account the invitation was sent to. `ACCEPTED` makes the user a participant of"
              + " the trip, who can then read it; `REJECTED` changes nothing else. An invitation"
              + " is answered once.")
  @ApiResponse(responseCode = "200", description = "The invitation with its new status")
  @ApiResponse(
      responseCode = "400",
      description = "Missing or unknown status, or `PENDING`",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = NOT_ADDRESSEE,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "No invitation has that id, or its trip was deleted",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "409",
      description = "The invitation was already answered, or the user is already in the trip",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PutMapping("/{id}/status")
  public ReceivedInvitationDTO respond(
      @PathVariable Long id, @Valid @RequestBody InvitationResponseRequest request) {
    return invitationService.respond(id, request.status());
  }
}
