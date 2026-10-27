package com.ruben.backend.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ruben.backend.activity.dto.BudgetConflictDTO;
import com.ruben.backend.activity.dto.OutsideDatesDTO;
import com.ruben.backend.activity.dto.OverlapDTO;
import com.ruben.backend.activity.dto.TripConflictsDTO;
import com.ruben.backend.activity.repository.ActivityRepository;
import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.service.TripAccessService;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("B6")
@ExtendWith(MockitoExtension.class)
class ConflictServiceUnitTest {

  private static final LocalDate DAY = LocalDate.of(2027, 7, 11);

  @Mock private ActivityRepository activityRepository;
  @Mock private TripAccessService tripAccessService;

  private ConflictService conflictService;
  private Trip trip;

  @BeforeEach
  void setUp() {
    conflictService = new ConflictService(activityRepository, tripAccessService);
    User ana = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
    trip = new Trip(ana, "Trip", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17));
  }

  @Test
  void findConflictsGathersTheThreeKindsFromTheQueries() {
    OverlapDTO overlap =
        new OverlapDTO(
            DAY,
            1L,
            "Tour",
            LocalTime.of(10, 0),
            LocalTime.of(12, 0),
            2L,
            "Lunch",
            LocalTime.of(11, 0),
            LocalTime.of(13, 0));
    OutsideDatesDTO outside = new OutsideDatesDTO(3L, "Early", LocalDate.of(2027, 7, 1));
    trip.setBudget(new BigDecimal("100.00"));
    when(tripAccessService.findForRead(1L)).thenReturn(trip);
    when(activityRepository.findOverlaps(1L)).thenReturn(List.of(overlap));
    when(activityRepository.findOutsideTrip(1L)).thenReturn(List.of(outside));
    when(activityRepository.sumCost(1L)).thenReturn(new BigDecimal("150.00"));

    TripConflictsDTO conflicts = conflictService.findConflicts(1L);

    assertThat(conflicts.overlaps()).containsExactly(overlap);
    assertThat(conflicts.outsideDates()).containsExactly(outside);
    assertThat(conflicts.budget())
        .isEqualTo(new BudgetConflictDTO(new BigDecimal("150.00"), new BigDecimal("100.00")));
  }

  @Test
  void findConflictsOfACleanTripIsEmptyWithoutBudgetConflict() {
    trip.setBudget(new BigDecimal("100.00"));
    when(tripAccessService.findForRead(1L)).thenReturn(trip);
    when(activityRepository.findOverlaps(1L)).thenReturn(List.of());
    when(activityRepository.findOutsideTrip(1L)).thenReturn(List.of());
    when(activityRepository.sumCost(1L)).thenReturn(null);

    TripConflictsDTO conflicts = conflictService.findConflicts(1L);

    assertThat(conflicts.overlaps()).isEmpty();
    assertThat(conflicts.outsideDates()).isEmpty();
    assertThat(conflicts.budget()).isNull();
  }

  @Test
  void findConflictsOfAMissingTripIsNotFoundBeforeAnyQuery() {
    when(tripAccessService.findForRead(1L)).thenThrow(new ResourceNotFoundException("Trip"));

    assertThatThrownBy(() -> conflictService.findConflicts(1L))
        .isInstanceOf(ResourceNotFoundException.class);
    verifyNoInteractions(activityRepository);
  }

  @Test
  void findConflictsOfAnotherUsersTripIsForbiddenBeforeAnyQuery() {
    when(tripAccessService.findForRead(1L)).thenThrow(new ForbiddenException("Not yours"));

    assertThatThrownBy(() -> conflictService.findConflicts(1L))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(activityRepository);
  }

  @Test
  void aTotalAboveTheBudgetIsAConflict() {
    assertThat(ConflictService.budgetConflict(new BigDecimal("100.01"), new BigDecimal("100.00")))
        .isEqualTo(new BudgetConflictDTO(new BigDecimal("100.01"), new BigDecimal("100.00")));
  }

  @Test
  void aTotalEqualToTheBudgetIsFineEvenWithADifferentScale() {
    assertThat(ConflictService.budgetConflict(new BigDecimal("100.0"), new BigDecimal("100.00")))
        .isNull();
  }

  @Test
  void aTotalBelowTheBudgetIsFine() {
    assertThat(ConflictService.budgetConflict(new BigDecimal("99.99"), new BigDecimal("100.00")))
        .isNull();
  }

  @Test
  void aTripWithoutBudgetHasNoBudgetConflict() {
    assertThat(ConflictService.budgetConflict(new BigDecimal("9999.00"), null)).isNull();
  }

  @Test
  void noCostAtAllIsNoBudgetConflictEvenWithAZeroBudget() {
    assertThat(ConflictService.budgetConflict(null, BigDecimal.ZERO)).isNull();
  }
}
