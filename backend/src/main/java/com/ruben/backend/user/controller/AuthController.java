package com.ruben.backend.user.controller;

import com.ruben.backend.common.security.JwtService;
import com.ruben.backend.user.dto.LoginRequest;
import com.ruben.backend.user.dto.LoginResult;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.service.AuthService;
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

  @PostMapping("/login")
  public ResponseEntity<UserDTO> login(@Valid @RequestBody LoginRequest request) {
    LoginResult result = authService.login(request);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, jwtService.authCookie(result.token()).toString())
        .body(result.user());
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout() {
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, jwtService.clearedCookie().toString())
        .build();
  }

  @GetMapping("/me")
  public UserDTO me() {
    return authService.getCurrentUser();
  }
}
