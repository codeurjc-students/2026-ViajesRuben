package com.ruben.backend.activity.dto;

import com.ruben.backend.activity.entity.Activity;
import com.ruben.backend.activity.entity.ActivityCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * What the API says about an activity, in lists and alone. The picture is served by `GET
 * /api/v1/images/{imageId}`, and `imageId` is null when there is none. The duration is not sent: it
 * is end time minus start time.
 */
public record ActivityDTO(
    Long id,
    Long tripId,
    Long authorId,
    String name,
    String description,
    String location,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    BigDecimal cost,
    ActivityCategory category,
    UUID imageId) {

  public static ActivityDTO from(Activity activity) {
    return new ActivityDTO(
        activity.getId(),
        activity.getTrip().getId(),
        activity.getAuthor().getId(),
        activity.getName(),
        activity.getDescription(),
        activity.getLocation(),
        activity.getDate(),
        activity.getStartTime(),
        activity.getEndTime(),
        activity.getCost(),
        activity.getCategory(),
        activity.getImage() == null ? null : activity.getImage().getId());
  }
}
