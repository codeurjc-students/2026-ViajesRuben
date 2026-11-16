package com.ruben.backend.trip.dto;

import java.time.LocalDate;

/**
 * What a user may choose when copying a public itinerary (I9). With a `startDate` the copy starts
 * that day, keeps the length of the original and every activity moves by the same number of days;
 * without it (no body, or null) the original dates are kept.
 */
public record CopyTripRequest(LocalDate startDate) {}
