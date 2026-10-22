package com.ruben.backend.common.security;

import java.time.Duration;
import java.time.Instant;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  public static final String COOKIE_NAME = "AuthToken";

  private final JwtEncoder jwtEncoder;
  private final JwtProperties properties;

  public JwtService(JwtEncoder jwtEncoder, JwtProperties properties) {
    this.jwtEncoder = jwtEncoder;
    this.properties = properties;
  }

  /** The subject is the user id, so changing the email (B3) does not invalidate the token. */
  public String issueToken(TripMateUserDetails user) {
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(String.valueOf(user.getId()))
            .issuedAt(now)
            .expiresAt(now.plus(properties.expiration()))
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  public ResponseCookie authCookie(String token) {
    return cookie(token, properties.expiration());
  }

  public ResponseCookie clearedCookie() {
    return cookie("", Duration.ZERO);
  }

  private static ResponseCookie cookie(String value, Duration maxAge) {
    return ResponseCookie.from(COOKIE_NAME, value)
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .path("/api")
        .maxAge(maxAge)
        .build();
  }
}
