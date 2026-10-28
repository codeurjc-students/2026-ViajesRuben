package com.ruben.backend.common;

import com.ruben.backend.activity.entity.Activity;
import com.ruben.backend.activity.entity.ActivityCategory;
import com.ruben.backend.activity.repository.ActivityRepository;
import com.ruben.backend.common.security.AdminProperties;
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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Loads the sample users, their trips and the itinerary of some trips that are not in the database
 * yet, plus the example trips of the public home page (B1), which belong to the administrator;
 * enabled with app.sample-data.enabled. The sample users are for development and demos: every one
 * logs in with app.sample-data.password (SAMPLE_USERS_PASSWORD, documented in the README), which is
 * encoded at start-up, so production must set SAMPLE_DATA=false. Runs after the administrator
 * account has been created.
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "app.sample-data.enabled", havingValue = "true")
public class DataLoader implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

  /** Folder of the classpath with the photos used as covers. */
  private static final String PHOTOS = "sample_images/";

  /**
   * A trip of the sample data. `budget`, `description` and `cover` (the file name of a photo in the
   * photos folder) are optional.
   */
  record SampleTrip(
      String name,
      String destination,
      LocalDate startDate,
      LocalDate endDate,
      String budget,
      String description,
      String cover) {}

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
   * The example trips of the public home page: the administrator's trips. There are four, so that
   * the home page, which shows three at a time, has a second page.
   */
  static final List<SampleTrip> SAMPLE_EXAMPLES =
      List.of(
          example(
              "Rome in five days",
              "Rome, Italy",
              LocalDate.of(2027, 5, 10),
              LocalDate.of(2027, 5, 14),
              "1200.00",
              "Ancient Rome, the Vatican and the best pasta of the city, at a relaxed pace.",
              "Rome_trip_example.png"),
          example(
              "Paris in four days",
              "Paris, France",
              LocalDate.of(2027, 6, 14),
              LocalDate.of(2027, 6, 17),
              "1500.00",
              "The classic Paris: museums, the Seine and the best viewpoints.",
              "Paris_trip_example.png"),
          example(
              "Maldives escape",
              "Maldives",
              LocalDate.of(2027, 9, 6),
              LocalDate.of(2027, 9, 12),
              "3200.00",
              "A week of beaches, snorkelling and sunsets on the water.",
              "Maldivas_trip_example.png"),
          example(
              "Tokyo highlights",
              "Tokyo, Japan",
              LocalDate.of(2027, 10, 4),
              LocalDate.of(2027, 10, 10),
              "2800.00",
              "Temples, markets and neon lights: the essentials of Tokyo in one week.",
              "Tokyo_trip_example.png"));

  /**
   * The itinerary of some sample trips, by trip name; the other trips start empty. The third day of
   * the Porto weekend is left empty on purpose: the UI tests create and delete an activity there.
   */
  static final Map<String, List<SampleActivity>> SAMPLE_ACTIVITIES =
      Map.of(
          "Rome in five days",
          List.of(
              activity(
                  0,
                  "Colosseum guided tour",
                  "Piazza del Colosseo",
                  9,
                  0,
                  11,
                  0,
                  "16.00",
                  ActivityCategory.SIGHTSEEING,
                  null),
              activity(
                  0,
                  "Lunch at Armando al Pantheon",
                  "Pantheon",
                  13,
                  0,
                  14,
                  30,
                  "35.00",
                  ActivityCategory.FOOD,
                  null),
              activity(
                  0,
                  "Roman Forum walk",
                  "Roman Forum",
                  16,
                  0,
                  18,
                  0,
                  "0.00",
                  ActivityCategory.SIGHTSEEING,
                  null),
              activity(
                  1,
                  "Vatican Museums and Sistine Chapel",
                  "Vatican City",
                  9,
                  0,
                  13,
                  0,
                  "20.00",
                  ActivityCategory.SIGHTSEEING,
                  null),
              activity(
                  1,
                  "Pasta lunch in Trastevere",
                  "Trastevere",
                  13,
                  30,
                  15,
                  0,
                  "22.00",
                  ActivityCategory.FOOD,
                  null),
              activity(
                  2,
                  "Borghese Gallery and Gardens",
                  "Villa Borghese",
                  10,
                  0,
                  13,
                  0,
                  "15.00",
                  ActivityCategory.SIGHTSEEING,
                  null),
              activity(
                  2,
                  "Trevi Fountain at sunset",
                  "Trevi",
                  19,
                  0,
                  20,
                  0,
                  null,
                  ActivityCategory.LEISURE,
                  null)),
          "Paris in four days",
          List.of(
              activity(
                  0,
                  "Louvre Museum visit",
                  "Rue de Rivoli",
                  9,
                  30,
                  13,
                  0,
                  "22.00",
                  ActivityCategory.SIGHTSEEING,
                  null),
              activity(
                  0,
                  "Seine river cruise",
                  "Pont Neuf",
                  18,
                  0,
                  19,
                  15,
                  "17.00",
                  ActivityCategory.LEISURE,
                  null),
              activity(
                  1,
                  "Montmartre food walk",
                  "Montmartre",
                  10,
                  0,
                  13,
                  0,
                  "45.00",
                  ActivityCategory.FOOD,
                  null),
              activity(
                  1,
                  "Sacré-Cœur at sunset",
                  "Montmartre",
                  19,
                  0,
                  20,
                  0,
                  null,
                  ActivityCategory.SIGHTSEEING,
                  null),
              activity(
                  2,
                  "Eiffel Tower summit",
                  "Champ de Mars",
                  10,
                  0,
                  12,
                  0,
                  "30.00",
                  ActivityCategory.SIGHTSEEING,
                  null),
              activity(
                  2,
                  "Dinner in Le Marais",
                  "Le Marais",
                  20,
                  0,
                  22,
                  0,
                  "50.00",
                  ActivityCategory.FOOD,
                  null)),
          "Maldives escape",
          List.of(
              activity(
                  0,
                  "Seaplane transfer to the resort",
                  "Malé",
                  10,
                  0,
                  11,
                  0,
                  "250.00",
                  ActivityCategory.TRANSPORT,
                  null),
              activity(
                  0,
                  "Sunset dinner on the beach",
                  "Resort beach",
                  19,
                  0,
                  21,
                  0,
                  "90.00",
                  ActivityCategory.FOOD,
                  null),
              activity(
                  1,
                  "Snorkelling on the house reef",
                  "House reef",
                  9,
                  30,
                  12,
                  0,
                  "40.00",
                  ActivityCategory.LEISURE,
                  null),
              activity(
                  2,
                  "Dolphin cruise",
                  "Open sea",
                  16,
                  0,
                  18,
                  0,
                  "60.00",
                  ActivityCategory.LEISURE,
                  null),
              activity(
                  2,
                  "Spa evening",
                  "Resort spa",
                  20,
                  0,
                  21,
                  30,
                  "80.00",
                  ActivityCategory.LEISURE,
                  null)),
          "Tokyo highlights",
          List.of(
              activity(
                  0,
                  "Senso-ji temple and Asakusa",
                  "Asakusa",
                  9,
                  0,
                  12,
                  0,
                  null,
                  ActivityCategory.SIGHTSEEING,
                  null),
              activity(
                  0,
                  "Sushi lunch at Tsukiji Outer Market",
                  "Tsukiji",
                  12,
                  30,
                  14,
                  0,
                  "40.00",
                  ActivityCategory.FOOD,
                  null),
              activity(
                  1,
                  "Shibuya Crossing and Meiji Shrine",
                  "Shibuya",
                  10,
                  0,
                  13,
                  0,
                  null,
                  ActivityCategory.SIGHTSEEING,
                  null),
              activity(
                  1,
                  "Ramen dinner in Shinjuku",
                  "Shinjuku",
                  19,
                  30,
                  21,
                  0,
                  "15.00",
                  ActivityCategory.FOOD,
                  null),
              activity(
                  2,
                  "Day trip to Mount Fuji",
                  "Kawaguchiko",
                  null,
                  null,
                  null,
                  null,
                  "90.00",
                  ActivityCategory.LEISURE,
                  null)),
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
  private final AdminProperties adminProperties;
  private final String password;

  public DataLoader(
      UserRepository userRepository,
      TripRepository tripRepository,
      ActivityRepository activityRepository,
      ImageRepository imageRepository,
      PasswordEncoder passwordEncoder,
      AdminProperties adminProperties,
      @Value("${app.sample-data.password}") String password) {
    this.userRepository = userRepository;
    this.tripRepository = tripRepository;
    this.activityRepository = activityRepository;
    this.imageRepository = imageRepository;
    this.passwordEncoder = passwordEncoder;
    this.adminProperties = adminProperties;
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
      loadTrips(user, sample.trips());
    }
    userRepository
        .findByEmail(adminProperties.email())
        .ifPresentOrElse(
            admin -> loadTrips(admin, SAMPLE_EXAMPLES),
            () -> log.warn("No administrator account, so the example trips were not loaded"));
  }

  /**
   * Creates the trips of `owner` that are missing, gives a cover to the ones that have a sample
   * cover and none yet, and loads the itineraries.
   */
  private void loadTrips(User owner, List<SampleTrip> samples) {
    List<Trip> trips =
        new ArrayList<>(
            tripRepository.findByCreatorIdAndNameIn(
                owner.getId(), samples.stream().map(SampleTrip::name).toList()));
    Set<String> existing = trips.stream().map(Trip::getName).collect(Collectors.toSet());
    List<Trip> missing =
        samples.stream()
            .filter(sample -> !existing.contains(sample.name()))
            .map(sample -> toTrip(owner, sample))
            .toList();
    trips.addAll(tripRepository.saveAll(missing));
    Map<String, SampleTrip> byName =
        samples.stream().collect(Collectors.toMap(SampleTrip::name, sample -> sample));
    trips.forEach(
        trip -> {
          loadCover(trip, byName.get(trip.getName()));
          loadItinerary(owner, trip);
        });
  }

  private static Trip toTrip(User owner, SampleTrip sample) {
    Trip trip =
        new Trip(owner, sample.name(), sample.destination(), sample.startDate(), sample.endDate());
    trip.setBudget(sample.budget() == null ? null : new BigDecimal(sample.budget()));
    trip.setDescription(sample.description());
    return trip;
  }

  /** A trip without a cover gets its sample photo, so databases loaded before it also get it. */
  private void loadCover(Trip trip, SampleTrip sample) {
    if (sample.cover() == null || trip.getCoverImage() != null) {
      return;
    }
    trip.setCoverImage(imageRepository.save(photo(sample.cover())));
    tripRepository.save(trip);
  }

  /** A photo of the sample photos folder of the classpath, with the type of its extension. */
  private static Image photo(String file) {
    String type = file.endsWith(".png") ? "image/png" : "image/jpeg";
    try (InputStream stream = new ClassPathResource(PHOTOS + file).getInputStream()) {
      return new Image(stream.readAllBytes(), type);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
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
    return new SampleTrip(
        name, destination, LocalDate.of(y1, m1, d1), LocalDate.of(y2, m2, d2), null, null, null);
  }

  private static SampleTrip example(
      String name,
      String destination,
      LocalDate startDate,
      LocalDate endDate,
      String budget,
      String description,
      String cover) {
    return new SampleTrip(name, destination, startDate, endDate, budget, description, cover);
  }
}
