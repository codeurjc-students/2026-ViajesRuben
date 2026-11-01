package com.ruben.backend.trip.dto;

import com.ruben.backend.trip.entity.Trip;
import java.time.LocalDate;

/**
 * A trip in the administrator's moderation list: what is needed to find it and to know whose it is.
 * No cover, budget or description; the details are read from the trip itself.
 */
public record AdminTripDTO(
    Long id,
    String name,
    String destination,
    LocalDate startDate,
    LocalDate endDate,
    Long creatorId,
    String creatorEmail) {

  public static AdminTripDTO from(Trip trip) {
    return new AdminTripDTO(
        trip.getId(),
        trip.getName(),
        trip.getDestination(),
        trip.getStartDate(),
        trip.getEndDate(),
        trip.getCreator().getId(),
        trip.getCreator().getEmail());
  }
}
