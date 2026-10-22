package com.ruben.backend.common.security;

import com.ruben.backend.common.error.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Answers 401 when authentication is missing or invalid. It also clears the cookie, so a stale,
 * invalid or blocked-user token cannot keep failing on every later request, public pages included.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ErrorResponseWriter errorResponseWriter;
  private final JwtService jwtService;

  public RestAuthenticationEntryPoint(
      ErrorResponseWriter errorResponseWriter, JwtService jwtService) {
    this.errorResponseWriter = errorResponseWriter;
    this.jwtService = jwtService;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    response.addHeader(HttpHeaders.SET_COOKIE, jwtService.clearedCookie().toString());
    String message =
        authException instanceof LockedException
            ? "Account is blocked"
            : "Authentication is required";
    errorResponseWriter.write(request, response, HttpStatus.UNAUTHORIZED, message);
  }
}
