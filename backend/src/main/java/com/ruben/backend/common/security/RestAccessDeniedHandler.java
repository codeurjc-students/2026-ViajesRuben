package com.ruben.backend.common.security;

import com.ruben.backend.common.error.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Answers 403 when an authenticated user lacks the role required by the URL. */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

  private final ErrorResponseWriter errorResponseWriter;

  public RestAccessDeniedHandler(ErrorResponseWriter errorResponseWriter) {
    this.errorResponseWriter = errorResponseWriter;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    errorResponseWriter.write(request, response, HttpStatus.FORBIDDEN, "Access is denied");
  }
}
