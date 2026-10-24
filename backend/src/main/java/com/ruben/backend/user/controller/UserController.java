package com.ruben.backend.user.controller;

import com.ruben.backend.common.error.ErrorResponse;
import com.ruben.backend.user.dto.RegisterRequest;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @Operation(
      summary = "Register a new account",
      description =
          "Public. The new user is always a regular user and is not logged in: call `POST"
              + " /api/v1/auth/login` afterwards. The `Location` header is the URL of the new"
              + " user; reading it is not available yet.")
  @ApiResponse(responseCode = "201", description = "The account was created")
  @ApiResponse(
      responseCode = "400",
      description = "Missing or invalid name, email or password",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "409",
      description = "The email is already registered",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PostMapping
  public ResponseEntity<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
    UserDTO user = userService.register(request);
    return ResponseEntity.created(URI.create("/api/v1/users/" + user.id())).body(user);
  }
}
