package com.ruben.backend.dto;

import java.time.LocalDate;

import com.ruben.backend.model.Trip;

public record TripDTO(
        Long id, String name, String destination, LocalDate startDate, LocalDate endDate) {

    public static TripDTO from(Trip trip) {
        return new TripDTO(trip.getId(), trip.getName(), trip.getDestination(),
                trip.getStartDate(), trip.getEndDate());
    }
}
