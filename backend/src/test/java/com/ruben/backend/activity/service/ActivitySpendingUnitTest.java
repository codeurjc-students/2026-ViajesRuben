package com.ruben.backend.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ruben.backend.activity.dto.CategorySpendingDTO;
import com.ruben.backend.activity.dto.DayCostDTO;
import com.ruben.backend.activity.dto.DaySpendingDTO;
import com.ruben.backend.activity.dto.TripSpendingDTO;
import com.ruben.backend.activity.entity.ActivityCategory;
import com.ruben.backend.activity.repository.ActivityRepository;
import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.image.service.ImageService;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.service.TripAccessService;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AuthService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("I5")
@ExtendWith(MockitoExtension.class)
class ActivitySpendingUnitTest {

  private static final LocalDate START = LocalDate.of(2027, 7, 10);
  private static final LocalDate END = LocalDate.of(2027, 7, 13);

  @Mock private ActivityRepository activityRepository;
  @Mock private UserRepository userRepository;
  @Mock private TripAccessService tripAccessService;
  @Mock private AuthService authService;
  @Mock private ImageService imageService;
  @Mock private ActivityLogService activityLogService;

  private ActivityService activityService;
  private Trip trip;

  @BeforeEach
  void setUp() {
    activityService =
        new ActivityService(
            activityRepository,
            userRepository,
            tripAccessService,
            authService,
            imageService,
            activityLogService);
    User ana = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
    trip = new Trip(ana, "Trip", "Rome", START, END);
    ReflectionTestUtils.setField(trip, "id", 1L);
  }

  private static DayCostDTO day(int dayOfMonth, String total) {
    return new DayCostDTO(LocalDate.of(2027, 7, dayOfMonth), new BigDecimal(total));
  }

  // --- fillDays: zero days and running sum ---

  @Test
  void fillDaysPutsZeroOnTheDaysWithoutCostAndAddsTheRunningSum() {
    List<DaySpendingDTO> days =
        ActivityService.fillDays(START, END, List.of(day(10, "20.00"), day(12, "5.50")));

    assertThat(days)
        .extracting(DaySpendingDTO::date)
        .containsExactly(START, START.plusDays(1), START.plusDays(2), END);
    assertThat(days)
        .extracting(d -> d.total().toPlainString())
        .containsExactly("20.00", "0", "5.50", "0");
    assertThat(days)
        .extracting(d -> d.cumulative().toPlainString())
        .containsExactly("20.00", "20.00", "25.50", "25.50");
  }

  @Test
  void fillDaysWithoutAnyCostGivesEveryTripDayWithZero() {
    List<DaySpendingDTO> days = ActivityService.fillDays(START, END, List.of());

    assertThat(days).hasSize(4);
    assertThat(days)
        .allSatisfy(
            d -> {
              assertThat(d.total()).isEqualByComparingTo("0");
              assertThat(d.cumulative()).isEqualByComparingTo("0");
            });
  }

  @Test
  void fillDaysOfAOneDayTripHasOneEntry() {
    List<DaySpendingDTO> days = ActivityService.fillDays(START, START, List.of(day(10, "9.99")));

    assertThat(days).hasSize(1);
    assertThat(days.get(0).cumulative()).isEqualByComparingTo("9.99");
  }

  @Test
  void fillDaysIncludesADayAfterTheEndSoTheLastSumIsTheTotal() {
    List<DaySpendingDTO> days =
        ActivityService.fillDays(START, END, List.of(day(10, "10.00"), day(15, "7.00")));

    assertThat(days).hasSize(6);
    assertThat(days.get(5).date()).isEqualTo(LocalDate.of(2027, 7, 15));
    assertThat(days.get(5).cumulative()).isEqualByComparingTo("17.00");
    assertThat(days.get(4).total()).isEqualByComparingTo("0");
  }

  @Test
  void fillDaysIncludesADayBeforeTheStart() {
    List<DaySpendingDTO> days =
        ActivityService.fillDays(START, END, List.of(day(8, "3.00"), day(11, "4.00")));

    assertThat(days.get(0).date()).isEqualTo(LocalDate.of(2027, 7, 8));
    assertThat(days).hasSize(6);
    assertThat(days.get(5).cumulative()).isEqualByComparingTo("7.00");
  }

  // --- findSpending ---

  @Test
  void findSpendingJoinsBudgetTotalCategoriesAndDays() {
    trip.setBudget(new BigDecimal("100.00"));
    List<CategorySpendingDTO> categories =
        List.of(
            new CategorySpendingDTO(ActivityCategory.FOOD, new BigDecimal("30.00")),
            new CategorySpendingDTO(null, new BigDecimal("5.00")));
    when(tripAccessService.findForRead(1L)).thenReturn(trip);
    when(activityRepository.sumCost(1L)).thenReturn(new BigDecimal("35.00"));
    when(activityRepository.sumCostByCategory(1L)).thenReturn(categories);
    when(activityRepository.sumCostByDay(1L)).thenReturn(List.of(day(11, "35.00")));

    TripSpendingDTO spending = activityService.findSpending(1L);

    assertThat(spending.budget()).isEqualByComparingTo("100.00");
    assertThat(spending.totalCost()).isEqualByComparingTo("35.00");
    assertThat(spending.byCategory()).isEqualTo(categories);
    assertThat(spending.byDay()).hasSize(4);
    assertThat(spending.byDay().get(3).cumulative()).isEqualByComparingTo("35.00");
  }

  @Test
  void findSpendingOfATripWithoutBudgetOrCostHasNullBudgetAndZeroTotal() {
    when(tripAccessService.findForRead(1L)).thenReturn(trip);
    when(activityRepository.sumCost(1L)).thenReturn(null);
    when(activityRepository.sumCostByCategory(1L)).thenReturn(List.of());
    when(activityRepository.sumCostByDay(1L)).thenReturn(List.of());

    TripSpendingDTO spending = activityService.findSpending(1L);

    assertThat(spending.budget()).isNull();
    assertThat(spending.totalCost()).isEqualByComparingTo("0");
    assertThat(spending.byCategory()).isEmpty();
    assertThat(spending.byDay()).hasSize(4);
  }

  @Test
  void findSpendingIsRefusedBeforeAnyQueryWhenTheTripCannotBeSeen() {
    when(tripAccessService.findForRead(1L)).thenThrow(new ForbiddenException("Not yours"));

    assertThatThrownBy(() -> activityService.findSpending(1L))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(activityRepository);
  }

  @Test
  void findSpendingOfAMissingTripIsNotFound() {
    when(tripAccessService.findForRead(1L))
        .thenThrow(new ResourceNotFoundException("Trip not found"));

    assertThatThrownBy(() -> activityService.findSpending(1L))
        .isInstanceOf(ResourceNotFoundException.class);
    verifyNoInteractions(activityRepository);
  }
}
