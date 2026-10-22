package com.ruben.backend.common.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

/** Reads the token from the HttpOnly cookie; a request without it is anonymous. */
@Component
public class CookieBearerTokenResolver implements BearerTokenResolver {

  @Override
  public String resolve(HttpServletRequest request) {
    Cookie cookie = WebUtils.getCookie(request, JwtService.COOKIE_NAME);
    if (cookie == null || cookie.getValue().isBlank()) {
      return null;
    }
    return cookie.getValue();
  }
}
