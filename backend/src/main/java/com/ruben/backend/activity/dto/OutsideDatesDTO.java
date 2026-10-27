package com.ruben.backend.activity.dto;

import java.time.LocalDate;

/** An activity whose date is before the trip starts or after it ends. */
public record OutsideDatesDTO(Long activityId, String name, LocalDate date) {}
