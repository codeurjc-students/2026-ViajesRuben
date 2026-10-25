package com.ruben.backend;

import com.ruben.backend.image.repository.ImageRepository;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.repository.UserRepository;
import org.springframework.stereotype.Component;

/**
 * Empties the database for the system tests, in foreign-key order (trips point at users and images,
 * users at images). A feature that adds an entity adds its repository here, once, and every test
 * that calls {@link #clear()} keeps working.
 */
@Component
public class DatabaseCleaner {

  private final TripRepository tripRepository;
  private final UserRepository userRepository;
  private final ImageRepository imageRepository;

  public DatabaseCleaner(
      TripRepository tripRepository,
      UserRepository userRepository,
      ImageRepository imageRepository) {
    this.tripRepository = tripRepository;
    this.userRepository = userRepository;
    this.imageRepository = imageRepository;
  }

  public void clear() {
    tripRepository.deleteAll();
    userRepository.deleteAll();
    imageRepository.deleteAll();
  }
}
