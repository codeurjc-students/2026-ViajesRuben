package com.ruben.backend.metrics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.ruben.backend.activity.repository.ActivityRepository;
import com.ruben.backend.metrics.dto.PlatformMetricsDTO;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("I6")
@ExtendWith(MockitoExtension.class)
class MetricsServiceUnitTest {

  @Mock private UserRepository userRepository;
  @Mock private TripRepository tripRepository;
  @Mock private ActivityRepository activityRepository;
  @InjectMocks private MetricsService service;

  @Test
  void getPlatformMetricsCountsRegularUsersTripsAndActivities() {
    when(userRepository.countByRole(Role.USER)).thenReturn(3L);
    when(tripRepository.count()).thenReturn(5L);
    when(activityRepository.count()).thenReturn(12L);

    PlatformMetricsDTO metrics = service.getPlatformMetrics();

    assertThat(metrics).isEqualTo(new PlatformMetricsDTO(3L, 5L, 12L));
  }

  @Test
  void getPlatformMetricsNeverCountsTheAdministratorAndLoadsNoRows() {
    when(userRepository.countByRole(Role.USER)).thenReturn(0L);
    when(tripRepository.count()).thenReturn(0L);
    when(activityRepository.count()).thenReturn(0L);

    PlatformMetricsDTO metrics = service.getPlatformMetrics();

    assertThat(metrics).isEqualTo(new PlatformMetricsDTO(0L, 0L, 0L));
    verify(userRepository).countByRole(Role.USER);
    verify(tripRepository).count();
    verify(activityRepository).count();
    verifyNoMoreInteractions(userRepository, tripRepository, activityRepository);
  }
}
