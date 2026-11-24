package com.ruben.backend.activity.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * What the activities of one day cost, as the database sums it; the days without cost are absent.
 */
public record DayCostDTO(LocalDate date, BigDecimal total) {}
