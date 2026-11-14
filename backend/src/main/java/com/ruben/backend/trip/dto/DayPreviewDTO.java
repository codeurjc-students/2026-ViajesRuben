package com.ruben.backend.trip.dto;

/** The first activity of one day of a public trip; `day` counts from 1 at the start date. */
public record DayPreviewDTO(int day, String activityName) {}
