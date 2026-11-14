package com.ruben.backend.trip.dto;

import java.util.List;

/**
 * A public itinerary in the public list: the trip plus a preview of its first days. Days without
 * activities are left out.
 */
public record PublicTripSummaryDTO(PublicTripDTO trip, List<DayPreviewDTO> firstDays) {}
