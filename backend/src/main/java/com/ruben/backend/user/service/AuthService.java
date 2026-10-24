package com.ruben.backend.user.service;

import com.ruben.backend.common.security.JwtService;
import com.ruben.backend.common.security.TripMateUserDetails;
import com.ruben.backend.user.dto.LoginRequest;
import com.ruben.backend.user.dto.LoginResult;
import com.ruben.backend.user.dto.UserDTO;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;

  public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
  }

  /**
   * Checks the credentials and issues a token.
   *
   * @throws org.springframework.security.authentication.BadCredentialsException wrong email or
   *     password
   * @throws org.springframework.security.authentication.LockedException correct credentials but the
   *     account is blocked
   */
  public LoginResult login(LoginRequest request) {
    Authentication authentication =
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                request.email(), request.password()));
    TripMateUserDetails user = (TripMateUserDetails) authentication.getPrincipal();
    return new LoginResult(toDto(user), jwtService.issueToken(user));
  }

  /** The user of the current request; only called on URLs that require authentication. */
  public UserDTO getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return toDto((TripMateUserDetails) authentication.getPrincipal());
  }

  private static UserDTO toDto(TripMateUserDetails user) {
    return new UserDTO(
        user.getId(),
        user.getName(),
        user.getUsername(),
        user.getRole(),
        user.getAvatarId(),
        user.getRegistrationDate());
  }
}
