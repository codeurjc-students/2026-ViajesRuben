package com.ruben.backend.activity.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * The data of the spending charts (I5). `budget` is null when the trip has none. `byCategory` has
 * only the categories with some cost, the biggest first. `byDay` has every day of the trip, with 0
 * on the days without cost, plus any day outside the trip's dates that has cost, so the last
 * `cumulative` is always `totalCost`.
 */
public record TripSpendingDTO(
    BigDecimal budget,
    BigDecimal totalCost,
    List<CategorySpendingDTO> byCategory,
    List<DaySpendingDTO> byDay) {}
