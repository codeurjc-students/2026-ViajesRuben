package com.ruben.backend.common;

import com.ruben.backend.activity.entity.Activity;
import com.ruben.backend.activity.entity.ActivityCategory;
import com.ruben.backend.activity.repository.ActivityRepository;
import com.ruben.backend.image.entity.Image;
import com.ruben.backend.image.repository.ImageRepository;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.repository.UserRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Loads the sample users, their trips and the itinerary of some trips that are not in the database
 * yet; enabled with app.sample-data.enabled. The sample users are for development and demos: every
 * one logs in with app.sample-data.password (SAMPLE_USERS_PASSWORD, documented in the README),
 * which is encoded at start-up, so production must set SAMPLE_DATA=false.
 */
@Component
@ConditionalOnProperty(name = "app.sample-data.enabled", havingValue = "true")
public class DataLoader implements CommandLineRunner {

  record SampleTrip(String name, String destination, LocalDate startDate, LocalDate endDate) {}

  /**
   * An activity of a sample trip: `day` counts from the first day of the trip, a null `start` means
   * the whole day, and a `picture` color means it gets a generated image.
   */
  record SampleActivity(
      int day,
      String name,
      String location,
      LocalTime start,
      LocalTime end,
      String cost,
      ActivityCategory category,
      Integer picture) {}

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

  /**
   * The itinerary of some sample trips, by trip name; the other trips start empty. The third day of
   * the Porto weekend is left empty on purpose: the UI tests create and delete an activity there.
   */
  static final Map<String, List<SampleActivity>> SAMPLE_ACTIVITIES =
      Map.of(
          "Lisbon getaway",
          List.of(
              activity(
                  0,
                  "Alfama walking tour",
                  "Alfama",
                  10,
                  0,
                  12,
                  30,
                  "0.00",
                  ActivityCategory.SIGHTSEEING,
                  0x2E6F9E),
              activity(
                  0,
                  "Lunch at Time Out Market",
                  "Cais do Sodré",
                  13,
                  30,
                  14,
                  30,
                  "18.50",
                  ActivityCategory.FOOD,
                  null),
              activity(
                  1,
                  "Belém Tower",
                  "Belém",
                  9,
                  30,
                  11,
                  0,
                  "6.00",
                  ActivityCategory.SIGHTSEEING,
                  0xC9822B),
              activity(
                  1,
                  "Pastéis de Belém tasting",
                  "Belém",
                  11,
                  15,
                  12,
                  0,
                  "7.50",
                  ActivityCategory.FOOD,
                  null),
              activity(
                  1,
                  "Tram 28 ride",
                  "Graça",
                  16,
                  0,
                  17,
                  0,
                  "3.10",
                  ActivityCategory.TRANSPORT,
                  null),
              activity(
                  2,
                  "Day trip to Sintra",
                  "Sintra",
                  null,
                  null,
                  null,
                  null,
                  "45.00",
                  ActivityCategory.LEISURE,
                  null),
              activity(
                  2, "Fado dinner", "Alfama", 20, 30, 23, 0, "55.00", ActivityCategory.FOOD, null)),
          "Summer in Rome",
          List.of(
              activity(
                  1,
                  "Colosseum guided tour",
                  "Piazza del Colosseo",
                  9,
                  0,
                  11,
                  0,
                  "16.00",
                  ActivityCategory.SIGHTSEEING,
                  0xB5651D),
              activity(
                  1,
                  "Pasta lunch in Trastevere",
                  "Trastevere",
                  13,
                  0,
                  14,
                  30,
                  "22.00",
                  ActivityCategory.FOOD,
                  null),
              activity(
                  2,
                  "Vatican Museums",
                  "Vatican City",
                  10,
                  0,
                  14,
                  0,
                  "20.00",
                  ActivityCategory.SIGHTSEEING,
                  null),
              activity(
                  4,
                  "Train to Naples",
                  "Roma Termini",
                  8,
                  15,
                  9,
                  45,
                  "14.00",
                  ActivityCategory.TRANSPORT,
                  null)),
          "Weekend in Porto",
          List.of(
              activity(
                  0,
                  "Ribeira walk",
                  "Ribeira",
                  10,
                  0,
                  12,
                  0,
                  null,
                  ActivityCategory.SIGHTSEEING,
                  null),
              activity(
                  0,
                  "Francesinha lunch",
                  "Baixa",
                  13,
                  0,
                  14,
                  0,
                  "12.50",
                  ActivityCategory.FOOD,
                  null),
              activity(
                  1,
                  "Livraria Lello",
                  "Rua das Carmelitas",
                  11,
                  0,
                  12,
                  0,
                  "8.00",
                  ActivityCategory.SIGHTSEEING,
                  null),
              activity(
                  1,
                  "Port wine cellar visit",
                  "Vila Nova de Gaia",
                  15,
                  0,
                  17,
                  0,
                  "15.00",
                  ActivityCategory.LEISURE,
                  0x7A2E4D)),
          "Andalusia route",
          List.of(
              activity(
                  0,
                  "Alcázar of Seville",
                  "Seville",
                  10,
                  0,
                  12,
                  30,
                  "14.50",
                  ActivityCategory.SIGHTSEEING,
                  null),
              activity(
                  1, "Tapas night", "Triana", 21, 0, 23, 0, "30.00", ActivityCategory.FOOD, null),
              activity(
                  3,
                  "Train to Granada",
                  "Santa Justa station",
                  9,
                  0,
                  11,
                  30,
                  "25.00",
                  ActivityCategory.TRANSPORT,
                  null)));

  private final UserRepository userRepository;
  private final TripRepository tripRepository;
  private final ActivityRepository activityRepository;
  private final ImageRepository imageRepository;
  private final PasswordEncoder passwordEncoder;
  private final String password;

  public DataLoader(
      UserRepository userRepository,
      TripRepository tripRepository,
      ActivityRepository activityRepository,
      ImageRepository imageRepository,
      PasswordEncoder passwordEncoder,
      @Value("${app.sample-data.password}") String password) {
    this.userRepository = userRepository;
    this.tripRepository = tripRepository;
    this.activityRepository = activityRepository;
    this.imageRepository = imageRepository;
    this.passwordEncoder = passwordEncoder;
    this.password = password;
  }

  @Override
  public void run(String... args) {
    String encodedPassword = passwordEncoder.encode(password);
    for (SampleUser sample : SAMPLE_USERS) {
      User user =
          userRepository
              .findByEmail(sample.email())
              .orElseGet(
                  () ->
                      userRepository.save(
                          new User(sample.name(), sample.email(), encodedPassword, Role.USER)));
      Map<String, Trip> trips = new HashMap<>();
      tripRepository
          .findByCreatorIdAndNameIn(
              user.getId(), sample.trips().stream().map(SampleTrip::name).toList())
          .forEach(trip -> trips.put(trip.getName(), trip));
      List<Trip> missing =
          sample.trips().stream()
              .filter(trip -> !trips.containsKey(trip.name()))
              .map(
                  trip ->
                      new Trip(
                          user, trip.name(), trip.destination(), trip.startDate(), trip.endDate()))
              .toList();
      tripRepository.saveAll(missing).forEach(trip -> trips.put(trip.getName(), trip));
      trips.values().forEach(trip -> loadItinerary(user, trip));
    }
  }

  /** Only a trip without any activity gets its sample itinerary, so nothing is ever duplicated. */
  private void loadItinerary(User author, Trip trip) {
    List<SampleActivity> samples = SAMPLE_ACTIVITIES.get(trip.getName());
    if (samples == null || activityRepository.existsByTripId(trip.getId())) {
      return;
    }
    activityRepository.saveAll(
        samples.stream().map(sample -> toActivity(author, trip, sample)).toList());
  }

  private Activity toActivity(User author, Trip trip, SampleActivity sample) {
    Activity activity =
        new Activity(trip, author, sample.name(), trip.getStartDate().plusDays(sample.day()));
    activity.setLocation(sample.location());
    activity.setStartTime(sample.start());
    activity.setEndTime(sample.end());
    activity.setCost(sample.cost() == null ? null : new BigDecimal(sample.cost()));
    activity.setCategory(sample.category());
    if (sample.picture() != null) {
      activity.setImage(imageRepository.save(new Image(solidPng(sample.picture()), "image/png")));
    }
    return activity;
  }

  /**
   * A small vertical gradient of one color, so the demo needs no binary files in the repository.
   */
  static byte[] solidPng(int rgb) {
    BufferedImage picture = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < picture.getHeight(); y++) {
      double light = 0.55 + 0.45 * y / picture.getHeight();
      int shaded =
          (((int) (((rgb >> 16) & 0xFF) * light)) << 16)
              | (((int) (((rgb >> 8) & 0xFF) * light)) << 8)
              | (int) ((rgb & 0xFF) * light);
      for (int x = 0; x < picture.getWidth(); x++) {
        picture.setRGB(x, y, shaded);
      }
    }
    try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
      ImageIO.write(picture, "png", bytes);
      return bytes.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static SampleActivity activity(
      int day,
      String name,
      String location,
      Integer startHour,
      Integer startMinute,
      Integer endHour,
      Integer endMinute,
      String cost,
      ActivityCategory category,
      Integer picture) {
    return new SampleActivity(
        day,
        name,
        location,
        startHour == null ? null : LocalTime.of(startHour, startMinute),
        endHour == null ? null : LocalTime.of(endHour, endMinute),
        cost,
        category,
        picture);
  }

  private static SampleTrip trip(
      String name, String destination, int y1, int m1, int d1, int y2, int m2, int d2) {
    return new SampleTrip(name, destination, LocalDate.of(y1, m1, d1), LocalDate.of(y2, m2, d2));
  }
}
