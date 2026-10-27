package com.ruben.backend.activity.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruben.backend.activity.entity.ActivityCategory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("B5")
class ActivityRequestUnitTest {

  private static final LocalDate DAY = LocalDate.of(2027, 5, 2);
  private static final LocalTime TEN = LocalTime.of(10, 0);
  private static final LocalTime NOON = LocalTime.of(12, 0);

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

  private static Set<String> invalidProperties(ActivityRequest request) {
    return validator.validate(request).stream()
        .map(violation -> violation.getPropertyPath().toString())
        .collect(Collectors.toSet());
  }

  private static ActivityRequest withName(String name) {
    return new ActivityRequest(name, DAY, null, null, null, null, null, null);
  }

  private static ActivityRequest withTimes(LocalTime start, LocalTime end) {
    return new ActivityRequest("Museum", DAY, start, end, null, null, null, null);
  }

  private static ActivityRequest withCost(String cost) {
    return new ActivityRequest("Museum", DAY, null, null, null, null, new BigDecimal(cost), null);
  }

  @Test
  void aCompleteRequestIsValid() {
    ActivityRequest request =
        new ActivityRequest(
            "Museum",
            DAY,
            TEN,
            NOON,
            "Lisbon",
            "Art",
            new BigDecimal("12.50"),
            ActivityCategory.SIGHTSEEING);

    assertThat(validator.validate(request)).isEmpty();
  }

  @Test
  void onlyTheNameAndTheDateAreRequired() {
    assertThat(validator.validate(withName("Museum"))).isEmpty();
  }

  @Test
  void aBlankNameIsInvalid() {
    assertThat(invalidProperties(withName("   "))).containsExactly("name");
    assertThat(invalidProperties(withName(null))).containsExactly("name");
  }

  @Test
  void theNameAllowsAtMost100Characters() {
    assertThat(validator.validate(withName("a".repeat(100)))).isEmpty();
    assertThat(invalidProperties(withName("a".repeat(101)))).containsExactly("name");
  }

  @Test
  void aMissingDateIsInvalid() {
    ActivityRequest request =
        new ActivityRequest("Museum", null, null, null, null, null, null, null);

    assertThat(invalidProperties(request)).containsExactly("date");
  }

  @Test
  void locationAndDescriptionHaveALimit() {
    ActivityRequest ok =
        new ActivityRequest(
            "Museum", DAY, null, null, "l".repeat(200), "d".repeat(2000), null, null);
    ActivityRequest tooLong =
        new ActivityRequest(
            "Museum", DAY, null, null, "l".repeat(201), "d".repeat(2001), null, null);

    assertThat(validator.validate(ok)).isEmpty();
    assertThat(invalidProperties(tooLong)).containsExactlyInAnyOrder("location", "description");
  }

  @Test
  void theCostCannotBeNegativeOrHaveMoreThanTwoDecimals() {
    assertThat(validator.validate(withCost("0.00"))).isEmpty();
    assertThat(validator.validate(withCost("99999999.99"))).isEmpty();
    assertThat(invalidProperties(withCost("-0.01"))).containsExactly("cost");
    assertThat(invalidProperties(withCost("1.234"))).containsExactly("cost");
    assertThat(invalidProperties(withCost("100000000"))).containsExactly("cost");
  }

  @Test
  void anEndAfterTheStartIsValid() {
    assertThat(validator.validate(withTimes(TEN, NOON))).isEmpty();
  }

  @Test
  void aStartWithoutEndAndNoTimesAtAllAreValid() {
    assertThat(validator.validate(withTimes(TEN, null))).isEmpty();
    assertThat(validator.validate(withTimes(null, null))).isEmpty();
  }

  @Test
  void anEndBeforeOrEqualToTheStartIsInvalid() {
    Set<ConstraintViolation<ActivityRequest>> before = validator.validate(withTimes(NOON, TEN));
    Set<ConstraintViolation<ActivityRequest>> equal = validator.validate(withTimes(TEN, TEN));

    assertThat(before)
        .extracting(ConstraintViolation::getMessage)
        .containsExactly("must be after startTime");
    assertThat(equal)
        .extracting(ConstraintViolation::getMessage)
        .containsExactly("must be after startTime");
    assertThat(invalidProperties(withTimes(NOON, TEN))).containsExactly("endTimeAfterStartTime");
  }

  @Test
  void anEndWithoutStartIsInvalid() {
    Set<ConstraintViolation<ActivityRequest>> violations =
        validator.validate(withTimes(null, NOON));

    assertThat(violations)
        .extracting(ConstraintViolation::getMessage)
        .containsExactly("requires startTime");
    assertThat(invalidProperties(withTimes(null, NOON))).containsExactly("endTimeWithStartTime");
  }
}
