package com.ruben.backend.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruben.backend.common.error.ErrorResponseWriter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.json.JsonMapper;

@Tag("B2")
class RestAccessDeniedHandlerUnitTest {

  private final RestAccessDeniedHandler handler =
      new RestAccessDeniedHandler(new ErrorResponseWriter(JsonMapper.builder().build()));

  @Test
  void aForbiddenRequestIsAnswered403WithTheErrorBody() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/users");
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.handle(request, response, new AccessDeniedException("denied"));

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentAsString())
        .contains("\"status\":403", "Access is denied", "/api/v1/admin/users");
    assertThat(response.getHeader("Set-Cookie")).isNull();
  }
}
