package com.ruben.backend.trip.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

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
import java.time.Instant;
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

/** The public itineraries as an anonymous visitor reads them (B1, I7). */
@Tag("B1")
@Tag("I7")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class PublicTripApiE2ETest {

  private static final String PASSWORD = "secret-pass";
  private static final String PUBLIC = "/api/v1/trips/public";
  private static final LocalDate START = LocalDate.of(2027, 7, 10);
  private static final Instant FIRST = Instant.parse("2027-01-01T10:00:00Z");

  @LocalServerPort private int port;

  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private TripRepository tripRepository;
  @Autowired private ActivityRepository activityRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AdminAccountInitializer adminAccountInitializer;

  private Trip[] published;
  private Trip draft;
  private String anaToken;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "https://localhost";
    RestAssured.port = port;
    RestAssured.useRelaxedHTTPSValidation();
    databaseCleaner.clear();
    adminAccountInitializer.run(new DefaultApplicationArguments());
    User admin = userRepository.findByEmail("admin@test.tripmate.com").orElseThrow();
    User ana =
        userRepository.save(
            new User("Ana García", "ana@example.com", passwordEncoder.encode(PASSWORD), Role.USER));
    published = new Trip[4];
    for (int i = 0; i < published.length; i++) {
      LocalDate start = START.plusDays(i * 10L);
      // Published by two different authors, the first one oldest and the last one newest.
      User author = i % 2 == 0 ? admin : ana;
      Trip trip = new Trip(author, "Public " + i, "City " + i, start, start.plusDays(4));
      trip.publish(FIRST.plusSeconds(i * 60L));
      published[i] = tripRepository.save(trip);
    }
    draft = tripRepository.save(new Trip(ana, "Ana's draft", "Lisbon", START, START.plusDays(4)));
    addActivity(published[0], 0, "Colosseum", LocalTime.of(9, 0));
    addActivity(published[0], 1, "Vatican", LocalTime.of(10, 0));
    addActivity(draft, 0, "Private plan", LocalTime.of(9, 0));
    anaToken =
        given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"ana@example.com\",\"password\":\"%s\"}".formatted(PASSWORD))
            .post("/api/v1/auth/login")
            .then()
            .statusCode(200)
            .extract()
            .cookie("AuthToken");
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
  void anAnonymousVisitorListsThePublicTripsNewestFirstPageByPage() {
    given()
        .get(PUBLIC + "?page=0&size=3")
        .then()
        .statusCode(200)
        .body("content", hasSize(3))
        .body("content.trip.name", contains("Public 3", "Public 2", "Public 1"))
        .body("totalElements", equalTo(4))
        .body("last", equalTo(false));
    given()
        .get(PUBLIC + "?page=1&size=3")
        .then()
        .statusCode(200)
        .body("content.trip.name", contains("Public 0"))
        .body("last", equalTo(true));
  }

  @Test
  void theListHasTripsOfSeveralAuthorsNeverAnUnpublishedOneAndPreviewsTheFirstDays() {
    given()
        .get(PUBLIC + "?size=10")
        .then()
        .statusCode(200)
        .body("content", hasSize(4))
        .body("content.trip.name", not(hasItem("Ana's draft")))
        .body("content.trip.authorName", hasItem("Ana García"))
        .body("content[3].firstDays.day", contains(1, 2))
        .body("content[3].firstDays.activityName", contains("Colosseum", "Vatican"))
        .body("content[0].firstDays", hasSize(0));
  }

  @Test
  void aPublicTripOfARegularUserIsListedNotOnlyTheAdministrators() {
    given()
        .get(PUBLIC + "?size=10")
        .then()
        .statusCode(200)
        .body("content.trip.name", hasItem("Public 1"));
  }

  @Test
  void thePublicResponsesNeverContainEmailsOrParticipants() {
    String list = given().get(PUBLIC + "?size=10").then().statusCode(200).extract().asString();
    String one =
        given()
            .get(PUBLIC + "/" + published[1].getId())
            .then()
            .statusCode(200)
            .extract()
            .asString();

    for (String body : new String[] {list, one}) {
      assertThat(body)
          .doesNotContain("@")
          .doesNotContain("email")
          .doesNotContain("participants")
          .doesNotContain("creatorId");
    }
    assertThat(one).contains("\"authorName\":\"Ana García\"");
  }

  @Test
  void anAnonymousVisitorReadsAPublicTripAndItsActivitiesOfOneDay() {
    given()
        .get(PUBLIC + "/" + published[0].getId())
        .then()
        .statusCode(200)
        .body("name", equalTo("Public 0"))
        .body("destination", equalTo("City 0"));
    given()
        .get(PUBLIC + "/" + published[0].getId() + "/activities?date=2027-07-11")
        .then()
        .statusCode(200)
        .body("content.name", contains("Vatican"))
        .body("totalElements", equalTo(1));
  }

  @Test
  void anUnpublishedTripIsNotFoundOnEveryPublicEndpointNeverForbidden() {
    given()
        .get(PUBLIC + "/" + draft.getId())
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .body("status", equalTo(404));
    given().get(PUBLIC + "/" + draft.getId() + "/activities").then().statusCode(404);
    given().get(PUBLIC + "/999999").then().statusCode(404);
    given().get(PUBLIC + "/999999/activities").then().statusCode(404);
  }

  @Test
  void unpublishingATripRemovesItFromTheCarouselListAndItsPublicReads() {
    long id = published[3].getId();
    given().get(PUBLIC + "?size=10").then().body("content.trip.name", hasItem("Public 3"));
    given()
        .cookie("AuthToken", anaToken)
        .contentType(ContentType.JSON)
        .body("{\"published\":false}")
        .put("/api/v1/trips/" + id + "/publication")
        .then()
        .statusCode(200);

    given()
        .get(PUBLIC + "?size=10")
        .then()
        .statusCode(200)
        .body("content", hasSize(3))
        .body("content.trip.name", not(hasItem("Public 3")));
    given().get(PUBLIC + "/" + id).then().statusCode(404);
    given().get(PUBLIC + "/" + id + "/activities").then().statusCode(404);
  }

  @Test
  void badParametersAreRejected() {
    given().get(PUBLIC + "/abc").then().statusCode(400);
    given().get(PUBLIC + "?sort=creator.password").then().statusCode(400);
    given()
        .get(PUBLIC + "/" + published[0].getId() + "/activities?date=yesterday")
        .then()
        .statusCode(400);
  }

  @Test
  void theOldExampleUrlsAreNoLongerPublic() {
    given().get("/api/v1/trips/examples").then().statusCode(401);
    given().get("/api/v1/trips/examples/" + published[0].getId()).then().statusCode(401);
  }

  @Test
  void everythingElseAboutTripsStaysAuthenticated() {
    long id = published[0].getId();
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
    given().post(PUBLIC).then().statusCode(401);
    given().delete(PUBLIC + "/" + id).then().statusCode(401);
  }
}
