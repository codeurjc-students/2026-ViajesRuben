package com.ruben.backend.invitation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruben.backend.invitation.entity.InvitationStatus;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("I2")
class InvitationResponseRequestUnitTest {

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

  @Test
  void acceptsAStatus() {
    assertThat(validator.validate(new InvitationResponseRequest(InvitationStatus.ACCEPTED)))
        .isEmpty();
    assertThat(validator.validate(new InvitationResponseRequest(InvitationStatus.REJECTED)))
        .isEmpty();
  }

  @Test
  void rejectsAMissingStatus() {
    assertThat(validator.validate(new InvitationResponseRequest(null))).hasSize(1);
  }
}
