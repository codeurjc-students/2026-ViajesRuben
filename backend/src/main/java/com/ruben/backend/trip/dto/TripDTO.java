package com.ruben.backend.trip.dto;

import com.ruben.backend.trip.entity.Trip;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * What the API says about a trip, in lists and alone. The cover bytes are served by `GET
 * /api/v1/images/{coverImageId}`, and `coverImageId` is null when the trip has no cover.
 */
public record TripDTO(
    Long id,
    Long creatorId,
    String name,
    String destination,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal budget,
    String description,
    UUID coverImageId) {

  public static TripDTO from(Trip trip) {
    return new TripDTO(
        trip.getId(),
        trip.getCreator().getId(),
        trip.getName(),
        trip.getDestination(),
        trip.getStartDate(),
        trip.getEndDate(),
        trip.getBudget(),
        trip.getDescription(),
        trip.getCoverImage() == null ? null : trip.getCoverImage().getId());
  }
}
