package com.ruben.backend.metrics.service;

import com.ruben.backend.activity.repository.ActivityRepository;
import com.ruben.backend.metrics.dto.PlatformMetricsDTO;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The platform totals for the administrator (I6); each one is a COUNT in the database. */
@Service
public class MetricsService {

  private final UserRepository userRepository;
  private final TripRepository tripRepository;
  private final ActivityRepository activityRepository;

  public MetricsService(
      UserRepository userRepository,
      TripRepository tripRepository,
      ActivityRepository activityRepository) {
    this.userRepository = userRepository;
    this.tripRepository = tripRepository;
    this.activityRepository = activityRepository;
  }

  /**
   * Users counts the regular accounts, blocked ones included and the administrator left out; trips
   * and activities count every row. The caller is checked by `SecurityConfig` (ADMIN only).
   */
  @Transactional(readOnly = true)
  public PlatformMetricsDTO getPlatformMetrics() {
    return new PlatformMetricsDTO(
        userRepository.countByRole(Role.USER), tripRepository.count(), activityRepository.count());
  }
}
