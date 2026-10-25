package com.ruben.backend.trip.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("B4")
class TripRequestUnitTest {

  private static final LocalDate START = LocalDate.of(2027, 5, 1);

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

  private static Set<String> invalidProperties(TripRequest request) {
    return validator.validate(request).stream()
        .map(violation -> violation.getPropertyPath().toString())
        .collect(java.util.stream.Collectors.toSet());
  }

  private static TripRequest request(String name, String destination, String description) {
    return new TripRequest(name, destination, START, START.plusDays(3), null, description);
  }

  @Test
  void aCompleteRequestIsValid() {
    TripRequest request =
        new TripRequest(
            "Lisbon", "Lisbon", START, START.plusDays(4), new BigDecimal("1200.50"), "Fun");

    assertThat(validator.validate(request)).isEmpty();
  }

  @Test
  void budgetAndDescriptionAreOptional() {
    assertThat(validator.validate(request("Lisbon", "Lisbon", null))).isEmpty();
  }

  @Test
  void blankOrMissingNameAndDestinationAreInvalid() {
    assertThat(invalidProperties(request("  ", "Lisbon", null))).containsExactly("name");
    assertThat(invalidProperties(request(null, "Lisbon", null))).containsExactly("name");
    assertThat(invalidProperties(request("Lisbon", "", null))).containsExactly("destination");
  }

  @Test
  void textsLongerThanTheColumnsAreInvalid() {
    assertThat(invalidProperties(request("n".repeat(101), "Lisbon", null))).containsExactly("name");
    assertThat(invalidProperties(request("Lisbon", "d".repeat(101), null)))
        .containsExactly("destination");
    assertThat(invalidProperties(request("Lisbon", "Lisbon", "x".repeat(2001))))
        .containsExactly("description");
    assertThat(validator.validate(request("n".repeat(100), "d".repeat(100), "x".repeat(2000))))
        .isEmpty();
  }

  @Test
  void missingDatesAreReportedOnlyByNotNull() {
    TripRequest noDates = new TripRequest("Lisbon", "Lisbon", null, null, null, null);

    assertThat(invalidProperties(noDates)).containsExactlyInAnyOrder("startDate", "endDate");
  }

  @Test
  void endBeforeStartIsInvalid() {
    TripRequest request =
        new TripRequest("Lisbon", "Lisbon", START, START.minusDays(1), null, null);

    Set<ConstraintViolation<TripRequest>> violations = validator.validate(request);

    assertThat(violations).hasSize(1);
    ConstraintViolation<TripRequest> violation = violations.iterator().next();
    assertThat(violation.getPropertyPath()).hasToString("endDateNotBeforeStartDate");
    assertThat(violation.getMessage()).isEqualTo("must not be before startDate");
  }

  @Test
  void aOneDayTripIsValid() {
    assertThat(validator.validate(new TripRequest("Lisbon", "Lisbon", START, START, null, null)))
        .isEmpty();
  }

  @Test
  void budgetMustBeAPositiveAmountWithTwoDecimalsAtMost() {
    assertThat(invalidProperties(withBudget("-0.01"))).containsExactly("budget");
    assertThat(invalidProperties(withBudget("10.123"))).containsExactly("budget");
    assertThat(invalidProperties(withBudget("1000000000"))).containsExactly("budget");
    assertThat(validator.validate(withBudget("0"))).isEmpty();
    assertThat(validator.validate(withBudget("99999999.99"))).isEmpty();
  }

  private static TripRequest withBudget(String budget) {
    return new TripRequest("Lisbon", "Lisbon", START, START, new BigDecimal(budget), null);
  }
}
