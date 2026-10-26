package com.ruben.backend.activity.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruben.backend.activity.entity.Activity;
import com.ruben.backend.activity.entity.ActivityCategory;
import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.image.entity.Image;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("B5")
class ActivityDTOUnitTest {

  private static Activity activity() {
    User ana = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
    Trip trip = new Trip(ana, "Trip", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17));
    ReflectionTestUtils.setField(trip, "id", 3L);
    Activity activity = new Activity(trip, ana, "Colosseum", LocalDate.of(2027, 7, 11));
    ReflectionTestUtils.setField(activity, "id", 5L);
    return activity;
  }

  @Test
  void mapsEveryFieldAndTheIdsOfTheRelatedEntities() {
    Activity activity = activity();
    Image image = new Image(ImageTestData.png(10), "image/png");
    UUID imageId = UUID.randomUUID();
    ReflectionTestUtils.setField(image, "id", imageId);
    activity.setImage(image);
    activity.setDescription("Guided tour");
    activity.setLocation("Piazza del Colosseo");
    activity.setStartTime(LocalTime.of(9, 30));
    activity.setEndTime(LocalTime.of(11, 0));
    activity.setCost(new BigDecimal("16.00"));
    activity.setCategory(ActivityCategory.SIGHTSEEING);

    ActivityDTO dto = ActivityDTO.from(activity);

    assertThat(dto)
        .isEqualTo(
            new ActivityDTO(
                5L,
                3L,
                7L,
                "Colosseum",
                "Guided tour",
                "Piazza del Colosseo",
                LocalDate.of(2027, 7, 11),
                LocalTime.of(9, 30),
                LocalTime.of(11, 0),
                new BigDecimal("16.00"),
                ActivityCategory.SIGHTSEEING,
                imageId));
  }

  @Test
  void anActivityWithoutOptionalFieldsHasNullsAndNoImage() {
    ActivityDTO dto = ActivityDTO.from(activity());

    assertThat(dto.imageId()).isNull();
    assertThat(dto.startTime()).isNull();
    assertThat(dto.cost()).isNull();
    assertThat(dto.category()).isNull();
    assertThat(dto.name()).isEqualTo("Colosseum");
  }
}
