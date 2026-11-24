package com.ruben.backend.activity.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One day of the spending line (I5): what was spent that day and the sum up to and including it.
 */
public record DaySpendingDTO(LocalDate date, BigDecimal total, BigDecimal cumulative) {}
