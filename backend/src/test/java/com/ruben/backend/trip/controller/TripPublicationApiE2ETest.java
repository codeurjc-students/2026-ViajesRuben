package com.ruben.backend.trip.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AdminAccountInitializer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
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

@Tag("I7")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class TripPublicationApiE2ETest {

  private static final String PASSWORD = "secret-pass";
  private static final String TRIPS = "/api/v1/trips";
  private static final String VALID_TRIP =
      "{\"name\":\"Lisbon trip\",\"destination\":\"Lisbon\",\"startDate\":\"2027-05-01\","
          + "\"endDate\":\"2027-05-05\"}";
  private static final String PUBLISH = "{\"published\":true}";
  private static final String UNPUBLISH = "{\"published\":false}";

  @LocalServerPort private int port;

  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AdminAccountInitializer adminAccountInitializer;

  private String anaToken;
  private String bobToken;
  private String adminToken;
  private long tripId;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "https://localhost";
    RestAssured.port = port;
    RestAssured.useRelaxedHTTPSValidation();
    databaseCleaner.clear();
    adminAccountInitializer.run(new DefaultApplicationArguments());
    newUser("Ana", "ana@example.com");
    newUser("Bob", "bob@example.com");
    anaToken = tokenOf("ana@example.com", PASSWORD);
    bobToken = tokenOf("bob@example.com", PASSWORD);
    adminToken = tokenOf("admin@test.tripmate.com", "Admin-test-1");
    tripId =
        json(anaToken)
            .body(VALID_TRIP)
            .post(TRIPS)
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
  }

  @AfterEach
  void tearDown() {
    RestAssured.reset();
  }

  private void newUser(String name, String email) {
    userRepository.save(new User(name, email, passwordEncoder.encode(PASSWORD), Role.USER));
  }

  private static String tokenOf(String email, String password) {
    return given()
        .contentType(ContentType.JSON)
        .body("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password))
        .post("/api/v1/auth/login")
        .then()
        .statusCode(200)
        .extract()
        .cookie("AuthToken");
  }

  private static RequestSpecification json(String token) {
    return given().cookie("AuthToken", token).contentType(ContentType.JSON);
  }

  private String publication() {
    return TRIPS + "/" + tripId + "/publication";
  }

  @Test
  void aNewTripIsNotPublished() {
    json(anaToken)
        .get(TRIPS + "/" + tripId)
        .then()
        .statusCode(200)
        .body("published", equalTo(false))
        .body("publishedAt", nullValue());
  }

  @Test
  void theCreatorPublishesTheTripAndItAppearsInThePublicList() {
    json(anaToken)
        .body(PUBLISH)
        .put(publication())
        .then()
        .statusCode(200)
        .body("id", equalTo((int) tripId))
        .body("published", equalTo(true))
        .body("publishedAt", notNullValue());

    given()
        .get(TRIPS + "/public/" + tripId)
        .then()
        .statusCode(200)
        .body("name", equalTo("Lisbon trip"))
        .body("authorName", equalTo("Ana"));
    json(anaToken)
        .get(TRIPS + "/" + tripId)
        .then()
        .body("published", equalTo(true))
        .body("publishedAt", notNullValue());
  }

  @Test
  void publishingTwiceIsIdempotentAndKeepsTheFirstDate() {
    String first =
        json(anaToken)
            .body(PUBLISH)
            .put(publication())
            .then()
            .statusCode(200)
            .extract()
            .path("publishedAt");

    String second =
        json(anaToken)
            .body(PUBLISH)
            .put(publication())
            .then()
            .statusCode(200)
            .body("published", equalTo(true))
            .extract()
            .path("publishedAt");

    assertThat(second).isEqualTo(first);
  }

  @Test
  void theCreatorUnpublishesTheTripAndItDisappearsFromThePublicEndpoints() {
    json(anaToken).body(PUBLISH).put(publication()).then().statusCode(200);

    json(anaToken)
        .body(UNPUBLISH)
        .put(publication())
        .then()
        .statusCode(200)
        .body("published", equalTo(false))
        .body("publishedAt", nullValue());

    given().get(TRIPS + "/public/" + tripId).then().statusCode(404);
    given().get(TRIPS + "/public/" + tripId + "/activities").then().statusCode(404);
    json(anaToken).get(TRIPS + "/" + tripId).then().statusCode(200);
  }

  @Test
  void unpublishingATripThatIsNotPublishedIsHarmless() {
    json(anaToken)
        .body(UNPUBLISH)
        .put(publication())
        .then()
        .statusCode(200)
        .body("published", equalTo(false));
  }

  @Test
  void anotherUserCannotPublishOrUnpublishTheTrip() {
    json(bobToken).body(PUBLISH).put(publication()).then().statusCode(403);
    json(anaToken).body(PUBLISH).put(publication()).then().statusCode(200);
    json(bobToken).body(UNPUBLISH).put(publication()).then().statusCode(403);

    given().get(TRIPS + "/public/" + tripId).then().statusCode(200);
  }

  @Test
  @Tag("I10")
  void theAdministratorCannotPublishAnotherUsersTrip() {
    json(adminToken).body(PUBLISH).put(publication()).then().statusCode(403);

    given().get(TRIPS + "/public/" + tripId).then().statusCode(404);
    json(anaToken).get(TRIPS + "/" + tripId).then().body("published", equalTo(false));
  }

  @Test
  @Tag("I10")
  void theAdministratorUnpublishesAnotherUsersTripAndItsCreatorKeepsIt() {
    json(anaToken).body(PUBLISH).put(publication()).then().statusCode(200);
    json(anaToken)
        .body("{\"name\":\"Belem\",\"date\":\"2027-05-02\"}")
        .post(TRIPS + "/" + tripId + "/activities")
        .then()
        .statusCode(201);

    json(adminToken)
        .body(UNPUBLISH)
        .put(publication())
        .then()
        .statusCode(200)
        .body("published", equalTo(false))
        .body("publishedAt", nullValue());

    given().get(TRIPS + "/public/" + tripId).then().statusCode(404);
    given().get(TRIPS + "/public/" + tripId + "/activities").then().statusCode(404);
    given()
        .get(TRIPS + "/public?search=Lisbon")
        .then()
        .statusCode(200)
        .body("totalElements", equalTo(0));
    given()
        .get(TRIPS + "/public?size=3")
        .then()
        .statusCode(200)
        .body("content.id", not(hasItem((int) tripId)));
    json(anaToken)
        .get(TRIPS + "/" + tripId)
        .then()
        .statusCode(200)
        .body("name", equalTo("Lisbon trip"))
        .body("published", equalTo(false));
    json(anaToken)
        .get(TRIPS + "/" + tripId + "/activities")
        .then()
        .statusCode(200)
        .body("content.name", hasItem("Belem"));
  }

  @Test
  @Tag("I10")
  void unpublishingATripThatIsNotPublishedIsHarmlessForTheAdministratorToo() {
    json(adminToken)
        .body(UNPUBLISH)
        .put(publication())
        .then()
        .statusCode(200)
        .body("published", equalTo(false));
  }

  @Test
  @Tag("I10")
  void anotherRegularUserStillCannotUnpublishAndAnAnonymousVisitorGets401() {
    json(anaToken).body(PUBLISH).put(publication()).then().statusCode(200);

    json(bobToken).body(UNPUBLISH).put(publication()).then().statusCode(403);
    given().contentType(ContentType.JSON).body(UNPUBLISH).put(publication()).then().statusCode(401);

    given().get(TRIPS + "/public/" + tripId).then().statusCode(200);
  }

  @Test
  @Tag("I10")
  void theAdministratorUnpublishingAMissingTripGets404() {
    json(adminToken).body(UNPUBLISH).put(TRIPS + "/999999/publication").then().statusCode(404);
  }

  @Test
  @Tag("I10")
  void theCreatorCanStillPublishAndUnpublishAfterAModeration() {
    json(anaToken).body(PUBLISH).put(publication()).then().statusCode(200);
    json(adminToken).body(UNPUBLISH).put(publication()).then().statusCode(200);

    json(anaToken)
        .body(PUBLISH)
        .put(publication())
        .then()
        .statusCode(200)
        .body("published", equalTo(true));
    json(anaToken).body(UNPUBLISH).put(publication()).then().statusCode(200);
  }

  @Test
  void anAnonymousVisitorCannotPublish() {
    given().contentType(ContentType.JSON).body(PUBLISH).put(publication()).then().statusCode(401);
  }

  @Test
  void aMissingTripIsNotFoundAndABadBodyOrIdIsRejected() {
    json(anaToken).body(PUBLISH).put(TRIPS + "/999999/publication").then().statusCode(404);
    json(anaToken).body("{}").put(publication()).then().statusCode(400);
    json(anaToken).body("{\"published\":null}").put(publication()).then().statusCode(400);
    json(anaToken).body("{\"published\":\"maybe\"}").put(publication()).then().statusCode(400);
    json(anaToken).body(PUBLISH).put(TRIPS + "/abc/publication").then().statusCode(400);
  }
}
