package com.ruben.backend.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Writes an {@link ErrorResponse} from the security filters, where no controller advice runs. */
@Component
public class ErrorResponseWriter {

  private final JsonMapper jsonMapper;

  public ErrorResponseWriter(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

  public void write(
      HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    jsonMapper.writeValue(
        response.getOutputStream(), ErrorResponse.of(status, message, request.getRequestURI()));
  }
}
