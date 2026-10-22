package com.ruben.backend.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

@Tag("B2")
class CookieBearerTokenResolverUnitTest {

  private final CookieBearerTokenResolver resolver = new CookieBearerTokenResolver();

  @Test
  void readsTheTokenFromTheAuthCookie() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie("AuthToken", "the-token"));

    assertThat(resolver.resolve(request)).isEqualTo("the-token");
  }

  @Test
  void aRequestWithoutCookiesIsAnonymous() {
    assertThat(resolver.resolve(new MockHttpServletRequest())).isNull();
  }

  @Test
  void otherCookiesAreIgnored() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie("theme", "dark"));

    assertThat(resolver.resolve(request)).isNull();
  }

  @Test
  void aBlankCookieIsAnonymous() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie("AuthToken", " "));

    assertThat(resolver.resolve(request)).isNull();
  }

  @Test
  void theAuthorizationHeaderIsNotAnAuthenticationSource() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer some-token");

    assertThat(resolver.resolve(request)).isNull();
  }
}
