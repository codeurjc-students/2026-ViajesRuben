package com.ruben.backend.user.controller;

import com.ruben.backend.common.error.ErrorResponse;
import com.ruben.backend.common.security.JwtService;
import com.ruben.backend.user.dto.LoginRequest;
import com.ruben.backend.user.dto.LoginResult;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;
  private final JwtService jwtService;

  public AuthController(AuthService authService, JwtService jwtService) {
    this.authService = authService;
    this.jwtService = jwtService;
  }

  @Operation(
      summary = "Log in with email and password",
      description =
          "Sets the HttpOnly, Secure `AuthToken` cookie that authenticates later requests.")
  @ApiResponse(responseCode = "200", description = "Logged in; the cookie is set")
  @ApiResponse(
      responseCode = "400",
      description = "Invalid body",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Wrong email or password",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = "Correct credentials but the account is blocked",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PostMapping("/login")
  public ResponseEntity<UserDTO> login(@Valid @RequestBody LoginRequest request) {
    LoginResult result = authService.login(request);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, jwtService.authCookie(result.token()).toString())
        .body(result.user());
  }

  @Operation(summary = "Log out", description = "Clears the `AuthToken` cookie.")
  @ApiResponse(responseCode = "204", description = "Logged out; the cookie is cleared")
  @PostMapping("/logout")
  public ResponseEntity<Void> logout() {
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, jwtService.clearedCookie().toString())
        .build();
  }

  @Operation(summary = "Get the logged-in user")
  @ApiResponse(responseCode = "200", description = "The user of the `AuthToken` cookie")
  @ApiResponse(
      responseCode = "401",
      description =
          "Missing, invalid or expired cookie, or the account is blocked; the cookie is cleared",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping("/me")
  public UserDTO me() {
    return authService.getCurrentUser();
  }
}
