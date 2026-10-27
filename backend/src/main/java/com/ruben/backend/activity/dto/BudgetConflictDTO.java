package com.ruben.backend.activity.dto;

import java.math.BigDecimal;

/** The activities cost more than the trip's budget. */
public record BudgetConflictDTO(BigDecimal totalCost, BigDecimal budget) {}
