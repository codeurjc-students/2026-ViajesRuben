package com.ruben.backend.activity.dto;

import java.math.BigDecimal;

/**
 * What a trip's activities cost and what is left of its budget. `remainingBudget` is the budget
 * minus `totalCost`, negative when the activities cost more, and null when the trip has no budget.
 */
public record TripCostDTO(BigDecimal totalCost, BigDecimal remainingBudget) {}
