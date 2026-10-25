package com.ruben.backend.trip.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The editable details of a trip, for creating it and for replacing it. `budget` and `description`
 * are optional; the cover has its own sub-resource.
 */
public record TripRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 100) String destination,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal budget,
    @Size(max = 2000) String description) {

  /**
   * Cross-field rule: a one-day trip (equal dates) is valid. A missing date is reported by its own
   * `@NotNull`, so it counts as valid here.
   */
  @JsonIgnore
  @Schema(hidden = true)
  @AssertTrue(message = "must not be before startDate")
  public boolean isEndDateNotBeforeStartDate() {
    return startDate == null || endDate == null || !endDate.isBefore(startDate);
  }
}
