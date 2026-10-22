package com.ruben.backend.common;

import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Loads the sample trips that are not in the database yet; enabled with app.sample-data.enabled.
 */
@Component
@ConditionalOnProperty(name = "app.sample-data.enabled", havingValue = "true")
public class DataLoader implements CommandLineRunner {

  private static final List<Trip> SAMPLE_TRIPS =
      List.of(
          new Trip("Trip 1", "Lisbon", LocalDate.of(2027, 5, 1), LocalDate.of(2027, 5, 5)),
          new Trip("Trip 2", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17)),
          new Trip("Trip 3", "Tokyo", LocalDate.of(2027, 9, 3), LocalDate.of(2027, 9, 15)),
          new Trip("Trip 4", "Paris", LocalDate.of(2027, 10, 8), LocalDate.of(2027, 10, 12)),
          new Trip("Trip 5", "Berlin", LocalDate.of(2027, 10, 20), LocalDate.of(2027, 10, 24)),
          new Trip("Trip 6", "Prague", LocalDate.of(2027, 11, 5), LocalDate.of(2027, 11, 9)),
          new Trip("Trip 7", "Vienna", LocalDate.of(2027, 11, 18), LocalDate.of(2027, 11, 21)),
          new Trip("Trip 8", "Amsterdam", LocalDate.of(2027, 12, 3), LocalDate.of(2027, 12, 6)),
          new Trip("Trip 9", "Madrid", LocalDate.of(2027, 12, 20), LocalDate.of(2027, 12, 27)),
          new Trip("Trip 10", "Athens", LocalDate.of(2028, 3, 14), LocalDate.of(2028, 3, 20)),
          new Trip("Trip 11", "Istanbul", LocalDate.of(2028, 4, 2), LocalDate.of(2028, 4, 9)),
          new Trip("Trip 12", "Reykjavik", LocalDate.of(2028, 6, 15), LocalDate.of(2028, 6, 22)));

  private final TripRepository tripRepository;

  public DataLoader(TripRepository tripRepository) {
    this.tripRepository = tripRepository;
  }

  @Override
  public void run(String... args) {
    Set<String> existing =
        tripRepository.findByNameIn(SAMPLE_TRIPS.stream().map(Trip::getName).toList()).stream()
            .map(Trip::getName)
            .collect(Collectors.toSet());
    tripRepository.saveAll(
        SAMPLE_TRIPS.stream()
            .filter(trip -> !existing.contains(trip.getName()))
            .map(
                trip ->
                    new Trip(
                        trip.getName(),
                        trip.getDestination(),
                        trip.getStartDate(),
                        trip.getEndDate()))
            .toList());
  }
}
