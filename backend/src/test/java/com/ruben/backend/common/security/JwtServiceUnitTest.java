package com.ruben.backend.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.UserStatus;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

@Tag("B2")
class JwtServiceUnitTest {

  private static final String SECRET = "QaaQj3xxiZt4l9E8wSVdofx96FjjBOwG6Y95zDIrTng=";
  private static final String OTHER_SECRET = "IqlIb/uwom5HUyTyHj+6CFj/Dj9htDhTQWs2vU3/4HA=";
  private static final Duration LIFETIME = Duration.ofHours(2);

  private final JwtConfig config = new JwtConfig();
  private final JwtProperties properties = new JwtProperties(SECRET, LIFETIME);
  private final JwtService jwtService = new JwtService(config.jwtEncoder(properties), properties);
  private final JwtDecoder decoder = config.jwtDecoder(properties);
  private final TripMateUserDetails user =
      new TripMateUserDetails(
          UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE));

  @Test
  void aTokenCarriesTheUserIdAndExpiresAfterTheConfiguredLifetime() {
    Jwt jwt = decoder.decode(jwtService.issueToken(user));

    assertThat(jwt.getSubject()).isEqualTo("7");
    assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(LIFETIME);
  }

  @Test
  void aTokenDoesNotExposeTheEmailOrTheRole() {
    Jwt jwt = decoder.decode(jwtService.issueToken(user));

    assertThat(jwt.getClaims()).doesNotContainKeys("email", "role", "roles", "scope");
  }

  @Test
  void aTokenSignedWithAnotherKeyIsRejected() {
    JwtProperties otherProperties = new JwtProperties(OTHER_SECRET, LIFETIME);
    JwtService otherService = new JwtService(config.jwtEncoder(otherProperties), otherProperties);

    String forged = otherService.issueToken(user);

    assertThatThrownBy(() -> decoder.decode(forged)).isInstanceOf(JwtException.class);
  }

  @Test
  void anExpiredTokenIsRejected() {
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject("7")
            .issuedAt(now.minus(Duration.ofHours(2)))
            .expiresAt(now.minus(Duration.ofHours(1)))
            .build();
    String expired =
        config
            .jwtEncoder(properties)
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();

    assertThatThrownBy(() -> decoder.decode(expired)).isInstanceOf(JwtException.class);
  }

  @Test
  void theAuthCookieIsHttpOnlySecureAndSameSiteStrict() {
    ResponseCookie cookie = jwtService.authCookie("the-token");

    assertThat(cookie.getName()).isEqualTo("AuthToken");
    assertThat(cookie.getValue()).isEqualTo("the-token");
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.isSecure()).isTrue();
    assertThat(cookie.getSameSite()).isEqualTo("Strict");
    assertThat(cookie.getPath()).isEqualTo("/api");
    assertThat(cookie.getMaxAge()).isEqualTo(LIFETIME);
  }

  @Test
  void theClearedCookieIsEmptyAndExpiresImmediatelyWithTheSameAttributes() {
    ResponseCookie cookie = jwtService.clearedCookie();

    assertThat(cookie.getName()).isEqualTo("AuthToken");
    assertThat(cookie.getValue()).isEmpty();
    assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.isSecure()).isTrue();
    assertThat(cookie.getSameSite()).isEqualTo("Strict");
    assertThat(cookie.getPath()).isEqualTo("/api");
  }
}
