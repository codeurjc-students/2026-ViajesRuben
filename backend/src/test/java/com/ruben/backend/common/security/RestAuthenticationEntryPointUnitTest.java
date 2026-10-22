package com.ruben.backend.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ruben.backend.common.error.ErrorResponseWriter;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.LockedException;
import tools.jackson.databind.json.JsonMapper;

@Tag("B2")
@ExtendWith(MockitoExtension.class)
class RestAuthenticationEntryPointUnitTest {

  private static final ResponseCookie CLEARED =
      ResponseCookie.from("AuthToken", "").path("/api").maxAge(Duration.ZERO).build();

  @Mock private JwtService jwtService;

  private RestAuthenticationEntryPoint entryPoint;
  private final MockHttpServletRequest request =
      new MockHttpServletRequest("GET", "/api/v1/auth/me");
  private final MockHttpServletResponse response = new MockHttpServletResponse();

  @BeforeEach
  void setUp() {
    when(jwtService.clearedCookie()).thenReturn(CLEARED);
    entryPoint =
        new RestAuthenticationEntryPoint(
            new ErrorResponseWriter(JsonMapper.builder().build()), jwtService);
  }

  @Test
  void anonymousAccessIsAnswered401AndClearsAnyCookie() throws Exception {
    entryPoint.commence(request, response, new InsufficientAuthenticationException("none"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentAsString()).contains("Authentication is required");
    assertThat(response.getHeader("Set-Cookie"))
        .startsWith("AuthToken=;")
        .contains("Path=/api", "Max-Age=0");
  }

  @Test
  void anInvalidTokenIsAnswered401AndClearsTheCookie() throws Exception {
    entryPoint.commence(request, response, new BadCredentialsException("bad token"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentAsString()).contains("Authentication is required");
    assertThat(response.getHeader("Set-Cookie")).contains("Max-Age=0");
  }

  @Test
  void aBlockedUserIsAnswered401WithTheBlockedMessageAndClearsTheCookie() throws Exception {
    entryPoint.commence(request, response, new LockedException("blocked"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentAsString()).contains("Account is blocked");
    assertThat(response.getHeader("Set-Cookie")).contains("Max-Age=0");
  }
}
