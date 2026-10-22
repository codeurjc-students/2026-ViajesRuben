package com.ruben.backend.common.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {

  private static final int MIN_KEY_BYTES = 32;

  @Bean
  JwtEncoder jwtEncoder(JwtProperties properties) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(properties)));
  }

  @Bean
  JwtDecoder jwtDecoder(JwtProperties properties) {
    return NimbusJwtDecoder.withSecretKey(secretKey(properties))
        .macAlgorithm(MacAlgorithm.HS256)
        .build();
  }

  private static SecretKey secretKey(JwtProperties properties) {
    byte[] bytes;
    try {
      bytes = Base64.getDecoder().decode(properties.secret());
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("app.jwt.secret must be a Base64 string", e);
    }
    if (bytes.length < MIN_KEY_BYTES) {
      throw new IllegalStateException("app.jwt.secret must decode to at least 32 bytes");
    }
    return new SecretKeySpec(bytes, "HmacSHA256");
  }
}
