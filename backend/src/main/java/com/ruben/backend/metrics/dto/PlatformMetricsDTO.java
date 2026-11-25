package com.ruben.backend.metrics.dto;

/**
 * The platform totals shown on the administrator's dashboard (I6): regular user accounts (blocked
 * ones included, the administrator not), trips (published or not) and activities.
 */
public record PlatformMetricsDTO(long users, long trips, long activities) {}
