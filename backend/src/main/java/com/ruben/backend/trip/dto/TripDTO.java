package com.ruben.backend.trip.dto;

import com.ruben.backend.trip.entity.Trip;
import java.time.LocalDate;

public record TripDTO(
    Long id, String name, String destination, LocalDate startDate, LocalDate endDate) {

  public static TripDTO from(Trip trip) {
    return new TripDTO(
        trip.getId(),
        trip.getName(),
        trip.getDestination(),
        trip.getStartDate(),
        trip.getEndDate());
  }
}
