package com.ruben.backend.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("B2")
class JwtConfigUnitTest {

  private final JwtConfig config = new JwtConfig();

  private static JwtProperties withSecret(String secret) {
    return new JwtProperties(secret, Duration.ofHours(1));
  }

  @Test
  void acceptsA256BitKey() {
    String secret = Base64.getEncoder().encodeToString(new byte[32]);

    assertThat(config.jwtEncoder(withSecret(secret))).isNotNull();
    assertThat(config.jwtDecoder(withSecret(secret))).isNotNull();
  }

  @Test
  void rejectsAKeyShorterThan256Bits() {
    String shortSecret = Base64.getEncoder().encodeToString(new byte[16]);

    assertThatThrownBy(() -> config.jwtEncoder(withSecret(shortSecret)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("at least 32 bytes");
  }

  @Test
  void rejectsASecretThatIsNotBase64() {
    assertThatThrownBy(() -> config.jwtDecoder(withSecret("***not base64***")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Base64");
  }
}
