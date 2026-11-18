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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
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
 * yet, plus the administrator's trips of the public home page (B1); some of the sample trips of
 * several authors are published as public itineraries (I7). Enabled with app.sample-data.enabled.
 * The sample users are for development and demos: every one logs in with app.sample-data.password
 * (SAMPLE_USERS_PASSWORD, documented in the README), which is encoded at start-up, so production
 * must set SAMPLE_DATA=false. Runs after the administrator account has been created.
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "app.sample-data.enabled", havingValue = "true")
public class DataLoader implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

  /** Folder of the classpath with the photos used as covers. */
  private static final String PHOTOS = "sample_images/";

  /**
   * Names of the sample trips that appear in more than one list below (the trips, the published
   * ones and the itineraries are matched by name).
   */
  private static final String LISBON_TRIP = "Lisbon getaway";

  private static final String ANDALUSIA_TRIP = "Andalusia route";
  private static final String ROME_EXAMPLE = "Roma en cinco días";
  private static final String MALDIVES_EXAMPLE = "Escapada a las Maldivas";
  private static final String TOKYO_EXAMPLE = "Lo mejor de Tokio";

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
                  trip(
                      LISBON_TRIP,
                      "Lisbon",
                      LocalDate.of(2027, Month.MAY, 1),
                      LocalDate.of(2027, Month.MAY, 5)),
                  trip(
                      "Summer in Rome",
                      "Rome",
                      LocalDate.of(2027, Month.JULY, 10),
                      LocalDate.of(2027, Month.JULY, 17)),
                  trip(
                      "Trip to Japan",
                      "Tokyo",
                      LocalDate.of(2027, Month.SEPTEMBER, 3),
                      LocalDate.of(2027, Month.SEPTEMBER, 15)),
                  trip(
                      "Long weekend in Paris",
                      "Paris",
                      LocalDate.of(2027, Month.OCTOBER, 8),
                      LocalDate.of(2027, Month.OCTOBER, 12)),
                  trip(
                      "Weekend in Berlin",
                      "Berlin",
                      LocalDate.of(2027, Month.OCTOBER, 20),
                      LocalDate.of(2027, Month.OCTOBER, 24)),
                  trip(
                      "Prague route",
                      "Prague",
                      LocalDate.of(2027, Month.NOVEMBER, 5),
                      LocalDate.of(2027, Month.NOVEMBER, 9)),
                  trip(
                      "Christmas in Vienna",
                      "Vienna",
                      LocalDate.of(2027, Month.NOVEMBER, 18),
                      LocalDate.of(2027, Month.NOVEMBER, 21)),
                  trip(
                      "Amsterdam canals",
                      "Amsterdam",
                      LocalDate.of(2027, Month.DECEMBER, 3),
                      LocalDate.of(2027, Month.DECEMBER, 6)),
                  trip(
                      "Madrid holidays",
                      "Madrid",
                      LocalDate.of(2027, Month.DECEMBER, 20),
                      LocalDate.of(2027, Month.DECEMBER, 27)),
                  trip(
                      "Week in Athens",
                      "Athens",
                      LocalDate.of(2028, Month.MARCH, 14),
                      LocalDate.of(2028, Month.MARCH, 20)),
                  trip(
                      "Spring in Istanbul",
                      "Istanbul",
                      LocalDate.of(2028, Month.APRIL, 2),
                      LocalDate.of(2028, Month.APRIL, 9)),
                  trip(
                      "Northern lights in Reykjavik",
                      "Reykjavik",
                      LocalDate.of(2028, Month.JUNE, 15),
                      LocalDate.of(2028, Month.JUNE, 22)))),
          new SampleUser(
              "Carlos Ruiz",
              "carlos@tripmate.com",
              List.of(
                  trip(
                      ANDALUSIA_TRIP,
                      "Seville",
                      LocalDate.of(2027, Month.APRIL, 5),
                      LocalDate.of(2027, Month.APRIL, 11)),
                  trip(
                      "Week in New York",
                      "New York",
                      LocalDate.of(2027, Month.AUGUST, 2),
                      LocalDate.of(2027, Month.AUGUST, 9)),
                  trip(
                      "Summer in Croatia",
                      "Split",
                      LocalDate.of(2027, Month.AUGUST, 20),
                      LocalDate.of(2027, Month.AUGUST, 30)))),
          new SampleUser(
              "Laura Pérez",
              "laura@tripmate.com",
              List.of(
                  trip(
                      "Weekend in Porto",
                      "Porto",
                      LocalDate.of(2027, Month.MARCH, 12),
                      LocalDate.of(2027, Month.MARCH, 14)),
                  trip(
                      "Alps in winter",
                      "Innsbruck",
                      LocalDate.of(2028, Month.JANUARY, 8),
                      LocalDate.of(2028, Month.JANUARY, 15)))));

  /**
   * The example trips of the public home page: the administrator's trips. There are three, which is
   * what the home page shows at a time, and every day of each one has activities.
   */
  static final List<SampleTrip> SAMPLE_EXAMPLES =
      List.of(
          example(
              ROME_EXAMPLE,
              "Roma, Italia",
              LocalDate.of(2027, Month.MAY, 10),
              LocalDate.of(2027, Month.MAY, 14),
              "1200.00",
              "La Roma antigua, el Vaticano y la mejor pasta de la ciudad, a un ritmo relajado.",
              "Rome_trip_example.png"),
          example(
              MALDIVES_EXAMPLE,
              "Maldivas",
              LocalDate.of(2027, Month.SEPTEMBER, 6),
              LocalDate.of(2027, Month.SEPTEMBER, 12),
              "3200.00",
              "Una semana de playas, snorkel y atardeceres sobre el agua.",
              "Maldivas_trip_example.png"),
          example(
              TOKYO_EXAMPLE,
              "Tokio, Japón",
              LocalDate.of(2027, Month.OCTOBER, 4),
              LocalDate.of(2027, Month.OCTOBER, 10),
              "2800.00",
              "Templos, mercados y luces de neón: lo esencial de Tokio en una semana.",
              "Tokyo_trip_example.png"));

  /**
   * The sample trips that are public itineraries (I7), newest publication first: the
   * administrator's three, so the first page of the home carousel is always the same, then one trip
   * of each of two sample users, so the carousel shows several authors. A trip is published only
   * when it is created, so one that somebody unpublished stays unpublished after a restart.
   */
  static final List<String> PUBLISHED_SAMPLES =
      List.of(ROME_EXAMPLE, MALDIVES_EXAMPLE, TOKYO_EXAMPLE, LISBON_TRIP, ANDALUSIA_TRIP);

  /**
   * The itinerary of some sample trips, by trip name; the other trips start empty. The third day of
   * the Porto weekend is left empty on purpose: the UI tests create and delete an activity there.
   */
  static final Map<String, List<SampleActivity>> SAMPLE_ACTIVITIES =
      Map.of(
          ROME_EXAMPLE,
          List.of(
              new SampleActivity(
                  0,
                  "Visita guiada al Coliseo",
                  "Piazza del Colosseo",
                  LocalTime.of(9, 0),
                  LocalTime.of(11, 0),
                  "16.00",
                  ActivityCategory.SIGHTSEEING,
                  null),
              new SampleActivity(
                  0,
                  "Comida en Armando al Pantheon",
                  "Panteón",
                  LocalTime.of(13, 0),
                  LocalTime.of(14, 30),
                  "35.00",
                  ActivityCategory.FOOD,
                  null),
              new SampleActivity(
                  0,
                  "Paseo por el Foro Romano",
                  "Foro Romano",
                  LocalTime.of(16, 0),
                  LocalTime.of(18, 0),
                  "0.00",
                  ActivityCategory.SIGHTSEEING,
                  null),
              new SampleActivity(
                  1,
                  "Museos Vaticanos y Capilla Sixtina",
                  "Ciudad del Vaticano",
                  LocalTime.of(9, 0),
                  LocalTime.of(13, 0),
                  "20.00",
                  ActivityCategory.SIGHTSEEING,
                  null),
              new SampleActivity(
                  1,
                  "Comida de pasta en Trastevere",
                  "Trastevere",
                  LocalTime.of(13, 30),
                  LocalTime.of(15, 0),
                  "22.00",
                  ActivityCategory.FOOD,
                  null),
              new SampleActivity(
                  2,
                  "Galería Borghese y jardines",
                  "Villa Borghese",
                  LocalTime.of(10, 0),
                  LocalTime.of(13, 0),
                  "15.00",
                  ActivityCategory.SIGHTSEEING,
                  null),
              new SampleActivity(
                  2,
                  "Fontana de Trevi al atardecer",
                  "Trevi",
                  LocalTime.of(19, 0),
                  LocalTime.of(20, 0),
                  null,
                  ActivityCategory.LEISURE,
                  null),
              new SampleActivity(
                  1,
                  "Subida a la cúpula de San Pedro",
                  "Ciudad del Vaticano",
                  LocalTime.of(15, 30),
                  LocalTime.of(17, 30),
                  "10.00",
                  ActivityCategory.SIGHTSEEING,
                  null),
              new SampleActivity(
                  3,
                  "Paseo por la Plaza de España y Via Condotti",
                  "Piazza di Spagna",
                  LocalTime.of(10, 0),
                  LocalTime.of(12, 30),
                  null,
                  ActivityCategory.LEISURE,
                  null),
              new SampleActivity(
                  3,
                  "Clase de cocina de pasta fresca",
                  "Campo de' Fiori",
                  LocalTime.of(17, 0),
                  LocalTime.of(20, 0),
                  "65.00",
                  ActivityCategory.FOOD,
                  null),
              new SampleActivity(
                  4,
                  "Museos Capitolinos",
                  "Piazza del Campidoglio",
                  LocalTime.of(9, 30),
                  LocalTime.of(12, 30),
                  "15.50",
                  ActivityCategory.SIGHTSEEING,
                  null),
              new SampleActivity(
                  4,
                  "Leonardo Express al aeropuerto",
                  "Roma Termini",
                  LocalTime.of(15, 0),
                  LocalTime.of(15, 45),
                  "14.00",
                  ActivityCategory.TRANSPORT,
                  null)),
          MALDIVES_EXAMPLE,
          List.of(
              new SampleActivity(
                  0,
                  "Traslado en hidroavión al resort",
                  "Malé",
                  LocalTime.of(10, 0),
                  LocalTime.of(11, 0),
                  "250.00",
                  ActivityCategory.TRANSPORT,
                  null),
              new SampleActivity(
                  0,
                  "Cena al atardecer en la playa",
                  "Playa del resort",
                  LocalTime.of(19, 0),
                  LocalTime.of(21, 0),
                  "90.00",
                  ActivityCategory.FOOD,
                  null),
              new SampleActivity(
                  1,
                  "Snorkel en el arrecife del resort",
                  "Arrecife del resort",
                  LocalTime.of(9, 30),
                  LocalTime.of(12, 0),
                  "40.00",
                  ActivityCategory.LEISURE,
                  null),
              new SampleActivity(
                  2,
                  "Crucero para ver delfines",
                  "Mar abierto",
                  LocalTime.of(16, 0),
                  LocalTime.of(18, 0),
                  "60.00",
                  ActivityCategory.LEISURE,
                  null),
              new SampleActivity(
                  2,
                  "Tarde de spa",
                  "Spa del resort",
                  LocalTime.of(20, 0),
                  LocalTime.of(21, 30),
                  "80.00",
                  ActivityCategory.LEISURE,
                  null),
              new SampleActivity(
                  1,
                  "Comida en la parrilla del resort",
                  "Restaurante del resort",
                  LocalTime.of(12, 30),
                  LocalTime.of(14, 0),
                  "35.00",
                  ActivityCategory.FOOD,
                  null),
              new SampleActivity(
                  3,
                  "Picnic en un banco de arena",
                  "Banco de arena",
                  LocalTime.of(10, 0),
                  LocalTime.of(14, 0),
                  "120.00",
                  ActivityCategory.LEISURE,
                  null),
              new SampleActivity(
                  3,
                  "Kayak al atardecer",
                  "Laguna del resort",
                  LocalTime.of(17, 0),
                  LocalTime.of(18, 30),
                  "30.00",
                  ActivityCategory.LEISURE,
                  null),
              new SampleActivity(
                  4,
                  "Bautismo de buceo",
                  "Centro de buceo",
                  LocalTime.of(9, 0),
                  LocalTime.of(12, 0),
                  "95.00",
                  ActivityCategory.LEISURE,
                  null),
              new SampleActivity(
                  4,
                  "Cena de marisco en el restaurante sobre el agua",
                  "Restaurante sobre el agua",
                  LocalTime.of(19, 30),
                  LocalTime.of(21, 30),
                  "85.00",
                  ActivityCategory.FOOD,
                  null),
              new SampleActivity(
                  5,
                  "Visita a una isla local y su mercado",
                  "Isla local",
                  LocalTime.of(10, 0),
                  LocalTime.of(13, 0),
                  "20.00",
                  ActivityCategory.SIGHTSEEING,
                  null),
              new SampleActivity(
                  5,
                  "Pesca al atardecer",
                  "Mar abierto",
                  LocalTime.of(16, 0),
                  LocalTime.of(18, 0),
                  "45.00",
                  ActivityCategory.LEISURE,
                  null),
              new SampleActivity(
                  6,
                  "Desayuno tranquilo y salida del hotel",
                  "Restaurante del resort",
                  LocalTime.of(8, 0),
                  LocalTime.of(9, 30),
                  null,
                  ActivityCategory.FOOD,
                  null),
              new SampleActivity(
                  6,
                  "Traslado en hidroavión al aeropuerto",
                  "Embarcadero del resort",
                  LocalTime.of(12, 0),
                  LocalTime.of(13, 0),
                  "250.00",
                  ActivityCategory.TRANSPORT,
                  null)),
          TOKYO_EXAMPLE,
          List.of(
              new SampleActivity(
                  0,
                  "Templo Senso-ji y Asakusa",
                  "Asakusa",
                  LocalTime.of(9, 0),
                  LocalTime.of(12, 0),
                  null,
                  ActivityCategory.SIGHTSEEING,
                  null),
              new SampleActivity(
                  0,
                  "Almuerzo de sushi en el mercado exterior de Tsukiji",
                  "Tsukiji",
                  LocalTime.of(12, 30),
                  LocalTime.of(14, 0),
                  "40.00",
                  ActivityCategory.FOOD,
                  null),
              new SampleActivity(
                  1,
                  "Cruce de Shibuya y santuario Meiji",
                  "Shibuya",
                  LocalTime.of(10, 0),
                  LocalTime.of(13, 0),
                  null,
                  ActivityCategory.SIGHTSEEING,
                  null),
              new SampleActivity(
                  1,
                  "Cena de ramen en Shinjuku",
                  "Shinjuku",
                  LocalTime.of(19, 30),
                  LocalTime.of(21, 0),
                  "15.00",
                  ActivityCategory.FOOD,
                  null),
              new SampleActivity(
                  2,
                  "Excursión al monte Fuji",
                  "Kawaguchiko",
                  null,
                  null,
                  "90.00",
                  ActivityCategory.LEISURE,
                  null),
              new SampleActivity(
                  0,
                  "Tokyo Skytree de noche",
                  "Sumida",
                  LocalTime.of(18, 0),
                  LocalTime.of(20, 0),
                  "22.00",
                  ActivityCategory.SIGHTSEEING,
                  null),
              new SampleActivity(
                  3,
                  "Museo de arte digital teamLab",
                  "Toyosu",
                  LocalTime.of(10, 0),
                  LocalTime.of(12, 30),
                  "35.00",
                  ActivityCategory.SIGHTSEEING,
                  null),
              new SampleActivity(
                  3,
                  "Akihabara: electrónica y salas recreativas",
                  "Akihabara",
                  LocalTime.of(14, 0),
                  LocalTime.of(17, 0),
                  null,
                  ActivityCategory.LEISURE,
                  null),
              new SampleActivity(
                  4,
                  "Jardines del Este del Palacio Imperial",
                  "Chiyoda",
                  LocalTime.of(9, 30),
                  LocalTime.of(11, 30),
                  null,
                  ActivityCategory.SIGHTSEEING,
                  null),
              new SampleActivity(
                  4,
                  "Parque Ueno y Museo Nacional",
                  "Ueno",
                  LocalTime.of(13, 0),
                  LocalTime.of(16, 0),
                  "10.00",
                  ActivityCategory.SIGHTSEEING,
                  null),
              new SampleActivity(
                  5,
                  "Excursión a Nikko",
                  "Nikko",
                  null,
                  null,
                  "55.00",
                  ActivityCategory.SIGHTSEEING,
                  null),
              new SampleActivity(
                  5,
                  "Cena en un izakaya de Golden Gai",
                  "Shinjuku",
                  LocalTime.of(20, 0),
                  LocalTime.of(22, 0),
                  "45.00",
                  ActivityCategory.FOOD,
                  null),
              new SampleActivity(
                  6,
                  "Últimas compras en Ginza",
                  "Ginza",
                  LocalTime.of(10, 0),
                  LocalTime.of(12, 0),
                  null,
                  ActivityCategory.LEISURE,
                  null),
              new SampleActivity(
                  6,
                  "Narita Express al aeropuerto",
                  "Estación de Tokio",
                  LocalTime.of(14, 0),
                  LocalTime.of(15, 30),
                  "30.00",
                  ActivityCategory.TRANSPORT,
                  null)),
          LISBON_TRIP,
          List.of(
              new SampleActivity(
                  0,
                  "Alfama walking tour",
                  "Alfama",
                  LocalTime.of(10, 0),
                  LocalTime.of(12, 30),
                  "0.00",
                  ActivityCategory.SIGHTSEEING,
                  0x2E6F9E),
              new SampleActivity(
                  0,
                  "Lunch at Time Out Market",
                  "Cais do Sodré",
                  LocalTime.of(13, 30),
                  LocalTime.of(14, 30),
                  "18.50",
                  ActivityCategory.FOOD,
                  null),
              new SampleActivity(
                  1,
                  "Belém Tower",
                  "Belém",
                  LocalTime.of(9, 30),
                  LocalTime.of(11, 0),
                  "6.00",
                  ActivityCategory.SIGHTSEEING,
                  0xC9822B),
              new SampleActivity(
                  1,
                  "Pastéis de Belém tasting",
                  "Belém",
                  LocalTime.of(11, 15),
                  LocalTime.of(12, 0),
                  "7.50",
                  ActivityCategory.FOOD,
                  null),
              new SampleActivity(
                  1,
                  "Tram 28 ride",
                  "Graça",
                  LocalTime.of(16, 0),
                  LocalTime.of(17, 0),
                  "3.10",
                  ActivityCategory.TRANSPORT,
                  null),
              new SampleActivity(
                  2,
                  "Day trip to Sintra",
                  "Sintra",
                  null,
                  null,
                  "45.00",
                  ActivityCategory.LEISURE,
                  null),
              new SampleActivity(
                  2,
                  "Fado dinner",
                  "Alfama",
                  LocalTime.of(20, 30),
                  LocalTime.of(23, 0),
                  "55.00",
                  ActivityCategory.FOOD,
                  null)),
          "Summer in Rome",
          List.of(
              new SampleActivity(
                  1,
                  "Colosseum guided tour",
                  "Piazza del Colosseo",
                  LocalTime.of(9, 0),
                  LocalTime.of(11, 0),
                  "16.00",
                  ActivityCategory.SIGHTSEEING,
                  0xB5651D),
              new SampleActivity(
                  1,
                  "Pasta lunch in Trastevere",
                  "Trastevere",
                  LocalTime.of(13, 0),
                  LocalTime.of(14, 30),
                  "22.00",
                  ActivityCategory.FOOD,
                  null),
              new SampleActivity(
                  2,
                  "Vatican Museums",
                  "Vatican City",
                  LocalTime.of(10, 0),
                  LocalTime.of(14, 0),
                  "20.00",
                  ActivityCategory.SIGHTSEEING,
                  null),
              new SampleActivity(
                  4,
                  "Train to Naples",
                  "Roma Termini",
                  LocalTime.of(8, 15),
                  LocalTime.of(9, 45),
                  "14.00",
                  ActivityCategory.TRANSPORT,
                  null)),
          "Weekend in Porto",
          List.of(
              new SampleActivity(
                  0,
                  "Ribeira walk",
                  "Ribeira",
                  LocalTime.of(10, 0),
                  LocalTime.of(12, 0),
                  null,
                  ActivityCategory.SIGHTSEEING,
                  null),
              new SampleActivity(
                  0,
                  "Francesinha lunch",
                  "Baixa",
                  LocalTime.of(13, 0),
                  LocalTime.of(14, 0),
                  "12.50",
                  ActivityCategory.FOOD,
                  null),
              new SampleActivity(
                  1,
                  "Livraria Lello",
                  "Rua das Carmelitas",
                  LocalTime.of(11, 0),
                  LocalTime.of(12, 0),
                  "8.00",
                  ActivityCategory.SIGHTSEEING,
                  null),
              new SampleActivity(
                  1,
                  "Port wine cellar visit",
                  "Vila Nova de Gaia",
                  LocalTime.of(15, 0),
                  LocalTime.of(17, 0),
                  "15.00",
                  ActivityCategory.LEISURE,
                  0x7A2E4D)),
          ANDALUSIA_TRIP,
          List.of(
              new SampleActivity(
                  0,
                  "Alcázar of Seville",
                  "Seville",
                  LocalTime.of(10, 0),
                  LocalTime.of(12, 30),
                  "14.50",
                  ActivityCategory.SIGHTSEEING,
                  null),
              new SampleActivity(
                  1,
                  "Tapas night",
                  "Triana",
                  LocalTime.of(21, 0),
                  LocalTime.of(23, 0),
                  "30.00",
                  ActivityCategory.FOOD,
                  null),
              new SampleActivity(
                  3,
                  "Train to Granada",
                  "Santa Justa station",
                  LocalTime.of(9, 0),
                  LocalTime.of(11, 30),
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
    Instant now = Instant.now();
    List<Trip> missing =
        samples.stream()
            .filter(sample -> !existing.contains(sample.name()))
            .map(sample -> toTrip(owner, sample, now))
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

  /**
   * A new trip of the sample data; the ones in {@link #PUBLISHED_SAMPLES} are published, one minute
   * apart from `now` so the public list has a fixed order.
   */
  private static Trip toTrip(User owner, SampleTrip sample, Instant now) {
    Trip trip =
        new Trip(owner, sample.name(), sample.destination(), sample.startDate(), sample.endDate());
    trip.setBudget(sample.budget() == null ? null : new BigDecimal(sample.budget()));
    trip.setDescription(sample.description());
    int position = PUBLISHED_SAMPLES.indexOf(sample.name());
    if (position >= 0) {
      trip.publish(now.minus(position, ChronoUnit.MINUTES));
    }
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

  private static SampleTrip trip(
      String name, String destination, LocalDate startDate, LocalDate endDate) {
    return new SampleTrip(name, destination, startDate, endDate, null, null, null);
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
