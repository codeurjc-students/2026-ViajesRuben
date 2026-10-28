package com.ruben.backend.trip.dto;

import java.util.List;

/**
 * An example trip in the public list: the trip plus a preview of its first days. Days without
 * activities are left out.
 */
public record ExampleTripDTO(TripDTO trip, List<DayPreviewDTO> firstDays) {}
