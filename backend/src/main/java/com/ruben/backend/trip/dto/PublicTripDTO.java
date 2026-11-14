package com.ruben.backend.trip.dto;

import com.ruben.backend.trip.entity.Trip;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A public itinerary as anyone sees it. Only what is meant to be public: the author's name, never
 * their email or id, and no participants.
 */
public record PublicTripDTO(
    Long id,
    String name,
    String destination,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal budget,
    String description,
    UUID coverImageId,
    String authorName,
    Instant publishedAt) {

  public static PublicTripDTO from(Trip trip) {
    return new PublicTripDTO(
        trip.getId(),
        trip.getName(),
        trip.getDestination(),
        trip.getStartDate(),
        trip.getEndDate(),
        trip.getBudget(),
        trip.getDescription(),
        trip.getCoverImage() == null ? null : trip.getCoverImage().getId(),
        trip.getCreator().getName(),
        trip.getPublishedAt());
  }
}
