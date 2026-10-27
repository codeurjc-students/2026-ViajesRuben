package com.ruben.backend.activity.service;

import com.ruben.backend.activity.dto.BudgetConflictDTO;
import com.ruben.backend.activity.dto.TripConflictsDTO;
import com.ruben.backend.activity.repository.ActivityRepository;
import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.service.TripAccessService;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The warnings about a trip's itinerary: overlapping activities, activities outside the trip's
 * dates and a total cost above the budget. They are only reported, never enforced. Overlaps, dates
 * and the sum are computed by the queries; the only comparison left to Java is {@link
 * #budgetConflict}.
 */
@Service
public class ConflictService {

  private final ActivityRepository activityRepository;
  private final TripAccessService tripAccessService;

  public ConflictService(
      ActivityRepository activityRepository, TripAccessService tripAccessService) {
    this.activityRepository = activityRepository;
    this.tripAccessService = tripAccessService;
  }

  /**
   * @throws ResourceNotFoundException no trip has that id
   * @throws ForbiddenException the user may not see the trip
   */
  @Transactional(readOnly = true)
  public TripConflictsDTO findConflicts(Long tripId) {
    Trip trip = tripAccessService.findForRead(tripId);
    return new TripConflictsDTO(
        activityRepository.findOverlaps(tripId),
        activityRepository.findOutsideTrip(tripId),
        budgetConflict(activityRepository.sumCost(tripId), trip.getBudget()));
  }

  /**
   * The conflict when the total is above the budget; none without a budget, and a total equal to
   * the budget is fine. A null total means no activity has a cost.
   */
  static BudgetConflictDTO budgetConflict(BigDecimal total, BigDecimal budget) {
    if (total == null || budget == null || total.compareTo(budget) <= 0) {
      return null;
    }
    return new BudgetConflictDTO(total, budget);
  }
}
