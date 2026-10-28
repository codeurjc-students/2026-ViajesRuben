package com.ruben.backend.trip.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.activity.entity.Activity;
import com.ruben.backend.activity.repository.ActivityRepository;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AdminAccountInitializer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

@Tag("B1")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ExampleTripApiE2ETest {

  private static final String PASSWORD = "secret-pass";
  private static final String EXAMPLES = "/api/v1/trips/examples";
  private static final LocalDate START = LocalDate.of(2027, 7, 10);

  @LocalServerPort private int port;

  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private TripRepository tripRepository;
  @Autowired private ActivityRepository activityRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AdminAccountInitializer adminAccountInitializer;

  private User admin;
  private Trip anasTrip;
  private Trip[] examples;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "https://localhost";
    RestAssured.port = port;
    RestAssured.useRelaxedHTTPSValidation();
    databaseCleaner.clear();
    adminAccountInitializer.run(new DefaultApplicationArguments());
    admin = userRepository.findByEmail("admin@test.tripmate.com").orElseThrow();
    User ana =
        userRepository.save(
            new User("Ana", "ana@example.com", passwordEncoder.encode(PASSWORD), Role.USER));
    examples = new Trip[4];
    for (int i = 0; i < examples.length; i++) {
      LocalDate start = START.plusDays(i * 10L);
      examples[i] =
          tripRepository.save(
              new Trip(admin, "Example " + i, "City " + i, start, start.plusDays(4)));
    }
    anasTrip = tripRepository.save(new Trip(ana, "Ana's trip", "Lisbon", START, START.plusDays(4)));
    addActivity(examples[0], 0, "Colosseum", LocalTime.of(9, 0));
    addActivity(examples[0], 1, "Vatican", LocalTime.of(10, 0));
    addActivity(anasTrip, 0, "Private plan", LocalTime.of(9, 0));
  }

  @AfterEach
  void tearDown() {
    RestAssured.reset();
  }

  private void addActivity(Trip trip, int day, String name, LocalTime start) {
    Activity activity =
        new Activity(trip, trip.getCreator(), name, trip.getStartDate().plusDays(day));
    activity.setStartTime(start);
    activityRepository.save(activity);
  }

  @Test
  void anAnonymousVisitorListsTheExampleTripsPageByPage() {
    given()
        .get(EXAMPLES + "?page=0&size=3")
        .then()
        .statusCode(200)
        .body("content", hasSize(3))
        .body("content.trip.name", contains("Example 0", "Example 1", "Example 2"))
        .body("totalElements", equalTo(4))
        .body("last", equalTo(false));
    given()
        .get(EXAMPLES + "?page=1&size=3")
        .then()
        .statusCode(200)
        .body("content.trip.name", contains("Example 3"))
        .body("last", equalTo(true));
  }

  @Test
  void theListNeverContainsAnotherUsersTripAndPreviewsTheFirstDays() {
    given()
        .get(EXAMPLES + "?size=10")
        .then()
        .statusCode(200)
        .body("content", hasSize(4))
        .body(
            "content.trip.name",
            org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("Ana's trip")))
        .body("content[0].firstDays.day", contains(1, 2))
        .body("content[0].firstDays.activityName", contains("Colosseum", "Vatican"))
        .body("content[1].firstDays", hasSize(0));
  }

  @Test
  void anAnonymousVisitorReadsAnExampleTripAndItsActivitiesOfOneDay() {
    given()
        .get(EXAMPLES + "/" + examples[0].getId())
        .then()
        .statusCode(200)
        .body("name", equalTo("Example 0"))
        .body("destination", equalTo("City 0"));
    given()
        .get(EXAMPLES + "/" + examples[0].getId() + "/activities?date=2027-07-11")
        .then()
        .statusCode(200)
        .body("content.name", contains("Vatican"))
        .body("totalElements", equalTo(1));
  }

  @Test
  void aTripThatIsNotAnExampleIsNotFoundOnEveryPublicEndpoint() {
    given()
        .get(EXAMPLES + "/" + anasTrip.getId())
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .body("status", equalTo(404));
    given().get(EXAMPLES + "/" + anasTrip.getId() + "/activities").then().statusCode(404);
    given().get(EXAMPLES + "/999999").then().statusCode(404);
    given().get(EXAMPLES + "/999999/activities").then().statusCode(404);
  }

  @Test
  void badParametersAreRejected() {
    given().get(EXAMPLES + "/abc").then().statusCode(400);
    given().get(EXAMPLES + "?sort=creator.password").then().statusCode(400);
    given()
        .get(EXAMPLES + "/" + examples[0].getId() + "/activities?date=yesterday")
        .then()
        .statusCode(400);
  }

  @Test
  void everythingElseAboutTripsStaysAuthenticated() {
    long id = examples[0].getId();
    given().get("/api/v1/trips").then().statusCode(401);
    given().get("/api/v1/trips/" + id).then().statusCode(401);
    given().get("/api/v1/trips/" + id + "/activities").then().statusCode(401);
    given().get("/api/v1/trips/" + id + "/conflicts").then().statusCode(401);
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"x\"}")
        .post("/api/v1/trips")
        .then()
        .statusCode(401);
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"x\"}")
        .put("/api/v1/trips/" + id)
        .then()
        .statusCode(401);
    given().delete("/api/v1/trips/" + id).then().statusCode(401);
    given().put("/api/v1/trips/" + id + "/cover").then().statusCode(401);
    given().post(EXAMPLES).then().statusCode(401);
    given().delete(EXAMPLES + "/" + id).then().statusCode(401);
  }
}
