package com.ruben.backend.activity.dto;

import com.ruben.backend.activity.entity.ActivityCategory;
import java.math.BigDecimal;

/**
 * What the activities of one category cost (I5). `category` is null for the activities that have
 * none.
 */
public record CategorySpendingDTO(ActivityCategory category, BigDecimal total) {}
