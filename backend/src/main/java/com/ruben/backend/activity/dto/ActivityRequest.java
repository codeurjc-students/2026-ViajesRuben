package com.ruben.backend.activity.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruben.backend.activity.entity.ActivityCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * The editable details of an activity, for creating it and for replacing it. Everything but `name`
 * and `date` is optional; the picture has its own sub-resource. Whether `date` is inside the trip
 * is checked by the service, which knows the trip.
 */
public record ActivityRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    @Size(max = 200) String location,
    @Size(max = 2000) String description,
    @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal cost,
    ActivityCategory category) {

  /** An end time alone says nothing: it needs a start time. */
  @JsonIgnore
  @Schema(hidden = true)
  @AssertTrue(message = "requires startTime")
  public boolean isEndTimeWithStartTime() {
    return endTime == null || startTime != null;
  }

  /** Same-day activities only: the end must be strictly after the start. */
  @JsonIgnore
  @Schema(hidden = true)
  @AssertTrue(message = "must be after startTime")
  public boolean isEndTimeAfterStartTime() {
    return startTime == null || endTime == null || endTime.isAfter(startTime);
  }
}
