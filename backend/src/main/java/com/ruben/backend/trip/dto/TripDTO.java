package com.ruben.backend.trip.dto;

import com.ruben.backend.trip.entity.Trip;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * What the API says about a trip, in lists and alone. The cover bytes are served by `GET
 * /api/v1/images/{coverImageId}`, and `coverImageId` is null when the trip has no cover.
 * `published` tells whether the trip is a public itinerary (I7); `publishedAt` is null while it is
 * not.
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
    UUID coverImageId,
    boolean published,
    Instant publishedAt) {

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
        trip.getCoverImage() == null ? null : trip.getCoverImage().getId(),
        trip.isPublished(),
        trip.getPublishedAt());
  }
}
