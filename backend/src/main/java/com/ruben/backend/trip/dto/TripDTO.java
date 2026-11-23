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
 * not. `copiedFromId` is the public trip this one was copied from (I9), or null. `participantCount`
 * is how many people joined the trip (I3); the creator is not counted, as in the list of
 * participants.
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
    Instant publishedAt,
    Long copiedFromId,
    long participantCount) {

  public static TripDTO from(Trip trip, long participantCount) {
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
        trip.getPublishedAt(),
        trip.getCopiedFrom() == null ? null : trip.getCopiedFrom().getId(),
        participantCount);
  }
}
