package com.ruben.backend.user.controller;

import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.ErrorResponse;
import com.ruben.backend.user.dto.AdminUserDTO;
import com.ruben.backend.user.dto.UpdateProfileRequest;
import com.ruben.backend.user.dto.UserStatusRequest;
import com.ruben.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** User management for the administrator; `/api/v1/admin/**` is restricted to the ADMIN role. */
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

  private static final String NOT_LOGGED_IN = "Not logged in";
  private static final String NOT_ADMIN =
      "The caller is not the administrator, or the target is the administrator's own account";
  private static final String NO_USER = "No user has that id";

  private final UserService userService;

  public AdminUserController(UserService userService) {
    this.userService = userService;
  }

  @Operation(
      summary = "List and search the users",
      description =
          "Administrator only. Regular users, not the administrator's own account. `search` keeps"
              + " the users whose name or email contains the text, ignoring case. Paginated:"
              + " `?page=0&size=10` (10 per page by default, 50 at most). Sorted by name by"
              + " default; `sort` accepts `id`, `name`, `email` and `registrationDate`.")
  @ApiResponse(responseCode = "200", description = "One page of users")
  @ApiResponse(
      responseCode = "400",
      description = "Invalid page, size or sort parameter",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = "The caller is not the administrator",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping
  public PageDTO<AdminUserDTO> getUsers(
      @RequestParam(required = false) String search,
      @ParameterObject
          @PageableDefault(
              size = 10,
              sort = {"name", "id"})
          Pageable pageable) {
    return userService.findPage(search, pageable);
  }

  @Operation(
      summary = "Edit a user's name and email",
      description =
          "Administrator only. Same rules as at registration: the email is stored in lower case"
              + " and must not belong to another account or to the administrator.")
  @ApiResponse(responseCode = "200", description = "The updated user")
  @ApiResponse(
      responseCode = "400",
      description = "Missing or invalid name or email",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = NOT_ADMIN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = NO_USER,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "409",
      description = "The email is already registered",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PutMapping("/{id}")
  public AdminUserDTO updateUser(
      @PathVariable Long id, @Valid @RequestBody UpdateProfileRequest request) {
    return userService.updateByAdmin(id, request);
  }

  @Operation(
      summary = "Block or unblock a user",
      description =
          "Administrator only. `BLOCKED` stops the user logging in and rejects their open session"
              + " on its next request; `ACTIVE` restores access. Setting the status the user"
              + " already has changes nothing.")
  @ApiResponse(responseCode = "200", description = "The user with the new status")
  @ApiResponse(
      responseCode = "400",
      description = "Missing or unknown status",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = NOT_ADMIN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = NO_USER,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PutMapping("/{id}/status")
  public AdminUserDTO changeStatus(
      @PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
    return userService.changeStatus(id, request.status());
  }
}
