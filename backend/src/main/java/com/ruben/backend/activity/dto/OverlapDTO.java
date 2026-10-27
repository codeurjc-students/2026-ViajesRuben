package com.ruben.backend.activity.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** Two activities of the same day whose time ranges intersect; each pair is reported once. */
public record OverlapDTO(
    LocalDate date,
    Long firstId,
    String firstName,
    LocalTime firstStart,
    LocalTime firstEnd,
    Long secondId,
    String secondName,
    LocalTime secondStart,
    LocalTime secondEnd) {}
