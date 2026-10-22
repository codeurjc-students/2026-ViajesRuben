package com.ruben.backend.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Tag("B2")
class ErrorResponseWriterUnitTest {

  private final JsonMapper jsonMapper = JsonMapper.builder().build();
  private final ErrorResponseWriter writer = new ErrorResponseWriter(jsonMapper);

  @Test
  void writesTheStatusAndAJsonBodyWithTheRequestPath() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
    MockHttpServletResponse response = new MockHttpServletResponse();

    writer.write(request, response, HttpStatus.UNAUTHORIZED, "Authentication is required");

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
    JsonNode body = jsonMapper.readTree(response.getContentAsString());
    assertThat(body.get("status").asInt()).isEqualTo(401);
    assertThat(body.get("error").asString()).isEqualTo("Unauthorized");
    assertThat(body.get("message").asString()).isEqualTo("Authentication is required");
    assertThat(body.get("path").asString()).isEqualTo("/api/v1/auth/me");
    assertThat(body.has("timestamp")).isTrue();
  }
}
