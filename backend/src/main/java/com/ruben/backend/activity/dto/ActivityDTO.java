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
 * is end time minus start time. `authorName` says who added it (I4); it is null in a public
 * itinerary, which never reveals the names of the trip's members.
 */
public record ActivityDTO(
    Long id,
    Long tripId,
    Long authorId,
    String authorName,
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
        activity.getAuthor().getName(),
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

  /** The same activity without the name of who added it, for the public itinerary. */
  public ActivityDTO withoutAuthorName() {
    return new ActivityDTO(
        id,
        tripId,
        authorId,
        null,
        name,
        description,
        location,
        date,
        startTime,
        endTime,
        cost,
        category,
        imageId);
  }
}
