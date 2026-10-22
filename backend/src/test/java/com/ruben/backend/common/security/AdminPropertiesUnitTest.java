package com.ruben.backend.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("B2")
class AdminPropertiesUnitTest {

  private static final String BCRYPT_HASH =
      "$2a$10$opop/gseD0EqqQJ4WJcxSeasiIejoCJni6fpKYxNFfDSgyVxq6VMS";

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  private Set<String> invalidFields(AdminProperties properties) {
    Set<ConstraintViolation<AdminProperties>> violations = validator.validate(properties);
    return violations.stream()
        .map(violation -> violation.getPropertyPath().toString())
        .collect(java.util.stream.Collectors.toSet());
  }

  @Test
  void aCompleteConfigurationWithABcryptHashIsValid() {
    assertThat(
            invalidFields(new AdminProperties("admin@tripmate.com", "Administrator", BCRYPT_HASH)))
        .isEmpty();
  }

  @Test
  void aPlainTextPasswordIsRejectedSoTheAppFailsFastAtStartUp() {
    assertThat(invalidFields(new AdminProperties("admin@tripmate.com", "Administrator", "admin")))
        .containsExactly("passwordHash");
  }

  @Test
  void aHashThatIsNotBcryptIsRejected() {
    String sha256Like = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8";

    assertThat(
            invalidFields(new AdminProperties("admin@tripmate.com", "Administrator", sha256Like)))
        .containsExactly("passwordHash");
  }

  @Test
  void anInvalidEmailAndABlankNameAreRejected() {
    assertThat(invalidFields(new AdminProperties("not-an-email", " ", BCRYPT_HASH)))
        .containsExactlyInAnyOrder("email", "name");
  }
}
