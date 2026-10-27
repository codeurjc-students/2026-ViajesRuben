package com.ruben.backend.activity.dto;

import java.util.List;

/**
 * The warnings about a trip's itinerary. They never block anything: the lists are empty and `budget`
 * is null when there is nothing to warn about.
 */
public record TripConflictsDTO(
    List<OverlapDTO> overlaps, List<OutsideDatesDTO> outsideDates, BudgetConflictDTO budget) {}
