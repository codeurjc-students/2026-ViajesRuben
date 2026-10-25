package com.ruben.backend.common;

import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Loads the sample users and their trips that are not in the database yet; enabled with
 * app.sample-data.enabled. The sample users are for development and demos: every one logs in with
 * the password documented in the README, so production must set SAMPLE_DATA=false.
 */
@Component
@ConditionalOnProperty(name = "app.sample-data.enabled", havingValue = "true")
public class DataLoader implements CommandLineRunner {

  /** BCrypt hash of the sample users' password, so the source holds no plain password. */
  static final String SAMPLE_USERS_HASH =
      "$2a$10$HaZS3CbIXPvVRWcZe.3ceeo32TvqmY.T1UgSGfCJ2mARRcWIh.eMu";

  record SampleTrip(String name, String destination, LocalDate startDate, LocalDate endDate) {}

  record SampleUser(String name, String email, List<SampleTrip> trips) {}

  static final List<SampleUser> SAMPLE_USERS =
      List.of(
          new SampleUser(
              "Ana García",
              "ana@tripmate.com",
              List.of(
                  trip("Lisbon getaway", "Lisbon", 2027, 5, 1, 2027, 5, 5),
                  trip("Summer in Rome", "Rome", 2027, 7, 10, 2027, 7, 17),
                  trip("Trip to Japan", "Tokyo", 2027, 9, 3, 2027, 9, 15),
                  trip("Long weekend in Paris", "Paris", 2027, 10, 8, 2027, 10, 12),
                  trip("Weekend in Berlin", "Berlin", 2027, 10, 20, 2027, 10, 24),
                  trip("Prague route", "Prague", 2027, 11, 5, 2027, 11, 9),
                  trip("Christmas in Vienna", "Vienna", 2027, 11, 18, 2027, 11, 21),
                  trip("Amsterdam canals", "Amsterdam", 2027, 12, 3, 2027, 12, 6),
                  trip("Madrid holidays", "Madrid", 2027, 12, 20, 2027, 12, 27),
                  trip("Week in Athens", "Athens", 2028, 3, 14, 2028, 3, 20),
                  trip("Spring in Istanbul", "Istanbul", 2028, 4, 2, 2028, 4, 9),
                  trip("Northern lights in Reykjavik", "Reykjavik", 2028, 6, 15, 2028, 6, 22))),
          new SampleUser(
              "Carlos Ruiz",
              "carlos@tripmate.com",
              List.of(
                  trip("Andalusia route", "Seville", 2027, 4, 5, 2027, 4, 11),
                  trip("Week in New York", "New York", 2027, 8, 2, 2027, 8, 9),
                  trip("Summer in Croatia", "Split", 2027, 8, 20, 2027, 8, 30))),
          new SampleUser(
              "Laura Pérez",
              "laura@tripmate.com",
              List.of(
                  trip("Weekend in Porto", "Porto", 2027, 3, 12, 2027, 3, 14),
                  trip("Alps in winter", "Innsbruck", 2028, 1, 8, 2028, 1, 15))));

  private final UserRepository userRepository;
  private final TripRepository tripRepository;

  public DataLoader(UserRepository userRepository, TripRepository tripRepository) {
    this.userRepository = userRepository;
    this.tripRepository = tripRepository;
  }

  @Override
  public void run(String... args) {
    for (SampleUser sample : SAMPLE_USERS) {
      User user =
          userRepository
              .findByEmail(sample.email())
              .orElseGet(
                  () ->
                      userRepository.save(
                          new User(sample.name(), sample.email(), SAMPLE_USERS_HASH, Role.USER)));
      Set<String> existing =
          tripRepository
              .findByCreatorIdAndNameIn(
                  user.getId(), sample.trips().stream().map(SampleTrip::name).toList())
              .stream()
              .map(Trip::getName)
              .collect(Collectors.toSet());
      tripRepository.saveAll(
          sample.trips().stream()
              .filter(trip -> !existing.contains(trip.name()))
              .map(
                  trip ->
                      new Trip(
                          user, trip.name(), trip.destination(), trip.startDate(), trip.endDate()))
              .toList());
    }
  }

  private static SampleTrip trip(
      String name, String destination, int y1, int m1, int d1, int y2, int m2, int d2) {
    return new SampleTrip(name, destination, LocalDate.of(y1, m1, d1), LocalDate.of(y2, m2, d2));
  }
}
