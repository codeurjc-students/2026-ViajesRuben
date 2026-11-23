package com.ruben.backend.activity.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruben.backend.activity.dto.ActivityRequest;
import com.ruben.backend.activity.entity.Activity;
import com.ruben.backend.activity.entity.ActivityCategory;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("I4")
class ActivityFieldChangesUnitTest {

  private static final LocalDate DAY = LocalDate.of(2027, 7, 11);

  private Activity activity;

  @BeforeEach
  void setUp() {
    User ana = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
    Trip trip = new Trip(ana, "Trip", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17));
    activity = new Activity(trip, ana, "Colosseum", DAY);
    activity.setDescription("Guided tour");
    activity.setLocation("Piazza");
    activity.setStartTime(LocalTime.of(10, 0));
    activity.setEndTime(LocalTime.of(12, 30));
    activity.setCost(new BigDecimal("16.50"));
    activity.setCategory(ActivityCategory.SIGHTSEEING);
  }

  /** The request that stores exactly what the activity has, with the given changes applied. */
  private ActivityRequest request(
      String name,
      LocalDate date,
      LocalTime start,
      LocalTime end,
      String location,
      String description,
      BigDecimal cost,
      ActivityCategory category) {
    return new ActivityRequest(name, date, start, end, location, description, cost, category);
  }

  private ActivityRequest same() {
    return request(
        "Colosseum",
        DAY,
        LocalTime.of(10, 0),
        LocalTime.of(12, 30),
        "Piazza",
        "Guided tour",
        new BigDecimal("16.50"),
        ActivityCategory.SIGHTSEEING);
  }

  @Test
  void aRequestEqualToTheActivityChangesNothing() {
    assertThat(ActivityFieldChanges.between(activity, same())).isEmpty();
  }

  @Test
  void textIsComparedAsItWillBeStoredTrimmedAndBlankAsNull() {
    ActivityRequest padded =
        request(
            "  Colosseum ",
            DAY,
            LocalTime.of(10, 0),
            LocalTime.of(12, 30),
            " Piazza  ",
            "  Guided tour ",
            new BigDecimal("16.50"),
            ActivityCategory.SIGHTSEEING);
    activity.setLocation(null);
    activity.setDescription(null);
    ActivityRequest blanks =
        request(
            "Colosseum",
            DAY,
            LocalTime.of(10, 0),
            LocalTime.of(12, 30),
            "   ",
            "",
            new BigDecimal("16.50"),
            ActivityCategory.SIGHTSEEING);

    assertThat(ActivityFieldChanges.between(activity, blanks)).isEmpty();
    activity.setLocation("Piazza");
    activity.setDescription("Guided tour");
    assertThat(ActivityFieldChanges.between(activity, padded)).isEmpty();
  }

  @Test
  void theCostIsComparedByAmountSoADifferentScaleIsNotAChange() {
    ActivityRequest otherScale =
        request(
            "Colosseum",
            DAY,
            LocalTime.of(10, 0),
            LocalTime.of(12, 30),
            "Piazza",
            "Guided tour",
            new BigDecimal("16.5"),
            ActivityCategory.SIGHTSEEING);

    assertThat(ActivityFieldChanges.between(activity, otherScale)).isEmpty();
  }

  @Test
  void eachFieldIsReportedWhenItAloneChanges() {
    assertThat(
            ActivityFieldChanges.between(
                activity,
                request(
                    "Forum",
                    DAY,
                    LocalTime.of(10, 0),
                    LocalTime.of(12, 30),
                    "Piazza",
                    "Guided tour",
                    new BigDecimal("16.50"),
                    ActivityCategory.SIGHTSEEING)))
        .containsExactly("name");
    assertThat(
            ActivityFieldChanges.between(
                activity,
                request(
                    "Colosseum",
                    DAY,
                    LocalTime.of(10, 0),
                    LocalTime.of(12, 30),
                    "Piazza",
                    "Night tour",
                    new BigDecimal("16.50"),
                    ActivityCategory.SIGHTSEEING)))
        .containsExactly("description");
    assertThat(
            ActivityFieldChanges.between(
                activity,
                request(
                    "Colosseum",
                    DAY,
                    LocalTime.of(10, 0),
                    LocalTime.of(12, 30),
                    "Forum",
                    "Guided tour",
                    new BigDecimal("16.50"),
                    ActivityCategory.SIGHTSEEING)))
        .containsExactly("location");
    assertThat(
            ActivityFieldChanges.between(
                activity,
                request(
                    "Colosseum",
                    DAY.plusDays(1),
                    LocalTime.of(10, 0),
                    LocalTime.of(12, 30),
                    "Piazza",
                    "Guided tour",
                    new BigDecimal("16.50"),
                    ActivityCategory.SIGHTSEEING)))
        .containsExactly("date");
    assertThat(
            ActivityFieldChanges.between(
                activity,
                request(
                    "Colosseum",
                    DAY,
                    LocalTime.of(9, 0),
                    LocalTime.of(12, 30),
                    "Piazza",
                    "Guided tour",
                    new BigDecimal("16.50"),
                    ActivityCategory.SIGHTSEEING)))
        .containsExactly("startTime");
    assertThat(
            ActivityFieldChanges.between(
                activity,
                request(
                    "Colosseum",
                    DAY,
                    LocalTime.of(10, 0),
                    LocalTime.of(13, 0),
                    "Piazza",
                    "Guided tour",
                    new BigDecimal("16.50"),
                    ActivityCategory.SIGHTSEEING)))
        .containsExactly("endTime");
    assertThat(
            ActivityFieldChanges.between(
                activity,
                request(
                    "Colosseum",
                    DAY,
                    LocalTime.of(10, 0),
                    LocalTime.of(12, 30),
                    "Piazza",
                    "Guided tour",
                    new BigDecimal("20.00"),
                    ActivityCategory.SIGHTSEEING)))
        .containsExactly("cost");
    assertThat(
            ActivityFieldChanges.between(
                activity,
                request(
                    "Colosseum",
                    DAY,
                    LocalTime.of(10, 0),
                    LocalTime.of(12, 30),
                    "Piazza",
                    "Guided tour",
                    new BigDecimal("16.50"),
                    ActivityCategory.FOOD)))
        .containsExactly("category");
  }

  @Test
  void aCostOrATimeThatAppearsOrDisappearsIsAChange() {
    ActivityRequest cleared =
        request("Colosseum", DAY, null, null, "Piazza", "Guided tour", null, null);

    assertThat(ActivityFieldChanges.between(activity, cleared))
        .containsExactly("startTime", "endTime", "cost", "category");

    activity.setStartTime(null);
    activity.setEndTime(null);
    activity.setCost(null);
    activity.setCategory(null);
    assertThat(ActivityFieldChanges.between(activity, cleared)).isEmpty();
    assertThat(ActivityFieldChanges.between(activity, same()))
        .containsExactly("startTime", "endTime", "cost", "category");
  }

  @Test
  void severalChangesComeInTheOrderOfTheForm() {
    ActivityRequest all =
        request(
            "Forum",
            DAY.plusDays(2),
            null,
            null,
            "Rome",
            "Other",
            new BigDecimal("1"),
            ActivityCategory.FOOD);

    assertThat(ActivityFieldChanges.between(activity, all))
        .containsExactly(
            "name", "description", "location", "date", "startTime", "endTime", "cost", "category");
  }

  @Test
  void blankToNullTrimsTextAndTurnsBlankIntoNull() {
    assertThat(ActivityFieldChanges.blankToNull(null)).isNull();
    assertThat(ActivityFieldChanges.blankToNull("  \t ")).isNull();
    assertThat(ActivityFieldChanges.blankToNull("  Piazza ")).isEqualTo("Piazza");
  }
}
