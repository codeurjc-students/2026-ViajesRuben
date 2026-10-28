package com.ruben.backend.activity.dto;

import java.time.LocalDate;

/** The first activity of one day of a trip, read by a query without loading the whole itinerary. */
public record FirstActivityDTO(Long tripId, LocalDate date, String name) {}
