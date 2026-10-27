package com.ruben.backend;

import com.ruben.backend.activity.repository.ActivityRepository;
import com.ruben.backend.image.repository.ImageRepository;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.repository.UserRepository;
import org.springframework.stereotype.Component;

/**
 * Empties the database for the system tests, in foreign-key order (activities point at trips, users
 * and images, trips at users and images, users at images). A feature that adds an entity adds its
 * repository here, once, and every test that calls {@link #clear()} keeps working.
 */
@Component
public class DatabaseCleaner {

  private final ActivityRepository activityRepository;
  private final TripRepository tripRepository;
  private final UserRepository userRepository;
  private final ImageRepository imageRepository;

  public DatabaseCleaner(
      ActivityRepository activityRepository,
      TripRepository tripRepository,
      UserRepository userRepository,
      ImageRepository imageRepository) {
    this.activityRepository = activityRepository;
    this.tripRepository = tripRepository;
    this.userRepository = userRepository;
    this.imageRepository = imageRepository;
  }

  public void clear() {
    activityRepository.deleteAll();
    tripRepository.deleteAll();
    userRepository.deleteAll();
    imageRepository.deleteAll();
  }
}
