package com.ruben.backend.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ruben.backend.activity.dto.FirstActivityDTO;
import com.ruben.backend.activity.service.ActivityService;
import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.BadRequestException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.trip.dto.DayPreviewDTO;
import com.ruben.backend.trip.dto.ExampleTripDTO;
import com.ruben.backend.trip.dto.TripDTO;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("B1")
@ExtendWith(MockitoExtension.class)
class ExampleTripServiceUnitTest {

  private static final LocalDate START = LocalDate.of(2027, 7, 10);

  @Mock private TripRepository tripRepository;
  @Mock private TripAccessService tripAccessService;
  @Mock private ActivityService activityService;

  private ExampleTripService service;
  private User admin;

  @BeforeEach
  void setUp() {
    service = new ExampleTripService(tripRepository, tripAccessService, activityService);
    admin = UserTestData.user(1L, "admin@tripmate.com", Role.ADMIN, UserStatus.ACTIVE);
  }

  private Trip trip(Long id, String name, LocalDate start) {
    Trip trip = new Trip(admin, name, "Rome", start, start.plusDays(6));
    ReflectionTestUtils.setField(trip, "id", id);
    return trip;
  }

  @Test
  void findPageListsTheAdministratorsTripsWithTheirFirstDaysNumberedFromTheStart() {
    Pageable pageable = PageRequest.of(0, 3, Sort.by("startDate", "id"));
    Trip rome = trip(10L, "Rome", START);
    Trip paris = trip(11L, "Paris", START.plusDays(30));
    when(tripRepository.findByCreatorRole(Role.ADMIN, pageable))
        .thenReturn(new PageImpl<>(List.of(rome, paris), pageable, 4));
    when(activityService.findFirstActivities(List.of(10L, 11L)))
        .thenReturn(
            List.of(
                new FirstActivityDTO(10L, START, "Colosseum"),
                new FirstActivityDTO(10L, START.plusDays(2), "Vatican"),
                new FirstActivityDTO(11L, START.plusDays(31), "Louvre")));

    PageDTO<ExampleTripDTO> page = service.findPage(pageable);

    assertThat(page.totalElements()).isEqualTo(4);
    assertThat(page.last()).isFalse();
    assertThat(page.content())
        .extracting(ExampleTripDTO::trip)
        .extracting(TripDTO::name)
        .containsExactly("Rome", "Paris");
    assertThat(page.content().get(0).firstDays())
        .containsExactly(new DayPreviewDTO(1, "Colosseum"), new DayPreviewDTO(3, "Vatican"));
    assertThat(page.content().get(1).firstDays()).containsExactly(new DayPreviewDTO(2, "Louvre"));
  }

  @Test
  void aTripWithoutActivitiesHasNoPreviewDays() {
    Pageable pageable = PageRequest.of(0, 3);
    when(tripRepository.findByCreatorRole(Role.ADMIN, pageable))
        .thenReturn(new PageImpl<>(List.of(trip(10L, "Rome", START)), pageable, 1));
    when(activityService.findFirstActivities(List.of(10L))).thenReturn(List.of());

    assertThat(service.findPage(pageable).content().get(0).firstDays()).isEmpty();
  }

  @Test
  void findPageRejectsSortingByAnotherPropertyBeforeQueryingAnything() {
    Pageable nested = PageRequest.of(0, 3, Sort.by("creator.password"));

    assertThatThrownBy(() -> service.findPage(nested))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Cannot sort by 'creator.password'");
    verifyNoInteractions(tripRepository, activityService);
  }

  @Test
  void findByIdReturnsTheExampleTrip() {
    when(tripAccessService.findExample(10L)).thenReturn(trip(10L, "Rome", START));

    assertThat(service.findById(10L).name()).isEqualTo("Rome");
  }

  @Test
  void findByIdOfATripThatIsNotAnExampleIsNotFound() {
    when(tripAccessService.findExample(5L)).thenThrow(new ResourceNotFoundException("Trip"));

    assertThatThrownBy(() -> service.findById(5L)).isInstanceOf(ResourceNotFoundException.class);
  }
}
