package com.ruben.backend.invitation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("I1")
class InvitationRequestUnitTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void createValidator() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void closeValidator() {
    factory.close();
  }

  private static boolean isValid(String email) {
    return validator.validate(new InvitationRequest(email)).isEmpty();
  }

  @Test
  void acceptsAnEmail() {
    assertThat(isValid("friend@example.com")).isTrue();
  }

  @Test
  void rejectsAMissingOrBlankEmail() {
    assertThat(isValid(null)).isFalse();
    assertThat(isValid("   ")).isFalse();
  }

  @Test
  void rejectsSomethingThatIsNotAnEmail() {
    assertThat(isValid("not-an-email")).isFalse();
  }

  @Test
  void rejectsAnEmailLongerThan255Characters() {
    assertThat(isValid("a".repeat(250) + "@b.com")).isFalse();
  }
}
