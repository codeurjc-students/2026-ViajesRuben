package com.ruben.backend.trip.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.activity.repository.ActivityRepository;
import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.image.repository.ImageRepository;
import com.ruben.backend.trip.repository.TripRepository;
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

@Tag("B8")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AdminTripApiE2ETest {

  private static final String PASSWORD = "secret-pass";
  private static final String ADMIN_TRIPS = "/api/v1/admin/trips";
  private static final String TRIPS = "/api/v1/trips";
  private static final String VALID_TRIP =
      "{\"name\":\"Hacked\",\"destination\":\"Nowhere\",\"startDate\":\"2027-05-01\","
          + "\"endDate\":\"2027-05-05\"}";

  @LocalServerPort private int port;

  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private UserRepository userRepository;
  @Autowired private TripRepository tripRepository;
  @Autowired private ActivityRepository activityRepository;
  @Autowired private ImageRepository imageRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AdminAccountInitializer adminAccountInitializer;

  private String anaToken;
  private String bobToken;
  private String adminToken;

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
  }

  @AfterEach
  void tearDown() {
    RestAssured.reset();
  }

  // --- helpers ---

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

  private static RequestSpecification as(String token) {
    return given().cookie("AuthToken", token);
  }

  private static RequestSpecification json(String token) {
    return as(token).contentType(ContentType.JSON);
  }

  private static long createTrip(String token, String name, String destination) {
    return json(token)
        .body(
            ("{\"name\":\"%s\",\"destination\":\"%s\",\"startDate\":\"2027-07-10\","
                    + "\"endDate\":\"2027-07-14\"}")
                .formatted(name, destination))
        .post(TRIPS)
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private static long createActivity(String token, long tripId, String name) {
    return json(token)
        .body("{\"name\":\"%s\",\"date\":\"2027-07-11\"}".formatted(name))
        .post(TRIPS + "/" + tripId + "/activities")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  /** Ana: Lisbon and Rome; Bob: Oslo; the administrator: Example. Newest is created last. */
  private void createSampleTrips() {
    createTrip(anaToken, "Lisbon", "Portugal");
    createTrip(anaToken, "Rome", "Italy");
    createTrip(bobToken, "Oslo", "Norway");
    createTrip(adminToken, "Example", "Anywhere");
  }

  // --- list and search ---

  @Test
  void listingRequiresLoginAndTheAdministratorRole() {
    given().get(ADMIN_TRIPS).then().statusCode(401).body("status", equalTo(401));
    as(anaToken).get(ADMIN_TRIPS).then().statusCode(403).body("status", equalTo(403));
    as(adminToken).get(ADMIN_TRIPS).then().statusCode(200);
  }

  @Test
  void theAdministratorListsTheTripsOfEveryUserNewestFirstWithTheirCreator() {
    createSampleTrips();

    as(adminToken)
        .get(ADMIN_TRIPS)
        .then()
        .statusCode(200)
        .body("content.name", contains("Example", "Oslo", "Rome", "Lisbon"))
        .body("content[1].creatorEmail", equalTo("bob@example.com"))
        .body("content[1].destination", equalTo("Norway"))
        .body("content[1].startDate", equalTo("2027-07-10"))
        .body("content[1].creatorId", not(nullValue()))
        .body("totalElements", equalTo(4))
        .body("last", equalTo(true));
  }

  @Test
  void searchMatchesNameDestinationOrCreatorEmailIgnoringCase() {
    createSampleTrips();

    as(adminToken)
        .queryParam("search", "LISB")
        .get(ADMIN_TRIPS)
        .then()
        .statusCode(200)
        .body("content.name", contains("Lisbon"));
    as(adminToken)
        .queryParam("search", "nOrWaY")
        .get(ADMIN_TRIPS)
        .then()
        .body("content.name", contains("Oslo"));
    as(adminToken)
        .queryParam("search", "  ANA@example  ")
        .get(ADMIN_TRIPS)
        .then()
        .body("content.name", containsInAnyOrder("Lisbon", "Rome"));
    as(adminToken)
        .queryParam("search", "nothing like this")
        .get(ADMIN_TRIPS)
        .then()
        .body("content", hasSize(0))
        .body("totalElements", equalTo(0));
    as(adminToken)
        .queryParam("search", "   ")
        .get(ADMIN_TRIPS)
        .then()
        .body("totalElements", equalTo(4));
  }

  @Test
  void theListIsPaginated() {
    createSampleTrips();

    as(adminToken)
        .queryParams("page", 0, "size", 3)
        .get(ADMIN_TRIPS)
        .then()
        .statusCode(200)
        .body("content", hasSize(3))
        .body("last", equalTo(false))
        .body("totalElements", equalTo(4));
    as(adminToken)
        .queryParams("page", 1, "size", 3)
        .get(ADMIN_TRIPS)
        .then()
        .body("content.name", contains("Lisbon"))
        .body("last", equalTo(true));
  }

  @Test
  void aSortThatIsNotWhitelistedIs400AndTheSizeIsCapped() {
    as(adminToken)
        .queryParam("sort", "creator.password")
        .get(ADMIN_TRIPS)
        .then()
        .statusCode(400)
        .body("message", equalTo("Cannot sort by 'creator.password'"));
    as(adminToken)
        .queryParam("size", 500)
        .get(ADMIN_TRIPS)
        .then()
        .statusCode(200)
        .body("size", equalTo(50));
  }

  // --- view ---

  @Test
  void theAdministratorReadsAnotherUsersTripItsActivitiesCostAndConflicts() {
    long trip = createTrip(anaToken, "Lisbon", "Portugal");
    long activity = createActivity(anaToken, trip, "Museum");
    String base = TRIPS + "/" + trip;

    as(adminToken).get(base).then().statusCode(200).body("name", equalTo("Lisbon"));
    as(adminToken)
        .get(base + "/activities")
        .then()
        .statusCode(200)
        .body("content.name", contains("Museum"));
    as(adminToken).get(base + "/activities/" + activity).then().statusCode(200);
    as(adminToken).get(base + "/activities/cost").then().statusCode(200);
    as(adminToken).get(base + "/conflicts").then().statusCode(200);
  }

  @Test
  void aRegularUserStillCannotReadOrDeleteAnotherUsersTrip() {
    long trip = createTrip(anaToken, "Lisbon", "Portugal");
    createActivity(anaToken, trip, "Museum");
    String base = TRIPS + "/" + trip;

    as(bobToken).get(base).then().statusCode(403);
    as(bobToken).get(base + "/activities").then().statusCode(403);
    as(bobToken).get(base + "/conflicts").then().statusCode(403);
    as(bobToken).delete(base).then().statusCode(403);
    given().delete(base).then().statusCode(401);
    as(anaToken).get(base).then().statusCode(200);
    assertThat(activityRepository.count()).isEqualTo(1);
  }

  // --- the administrator never edits ---

  @Test
  void theAdministratorCannotEditAnotherUsersTripOrItsActivities() {
    long trip = createTrip(anaToken, "Lisbon", "Portugal");
    long activity = createActivity(anaToken, trip, "Museum");
    String base = TRIPS + "/" + trip;

    json(adminToken).body(VALID_TRIP).put(base).then().statusCode(403);
    as(adminToken)
        .multiPart("file", "picture", ImageTestData.png(100), "image/png")
        .put(base + "/cover")
        .then()
        .statusCode(403);
    as(adminToken).delete(base + "/cover").then().statusCode(403);
    json(adminToken)
        .body("{\"name\":\"Extra\",\"date\":\"2027-07-12\"}")
        .post(base + "/activities")
        .then()
        .statusCode(403);
    json(adminToken)
        .body("{\"name\":\"Changed\",\"date\":\"2027-07-12\"}")
        .put(base + "/activities/" + activity)
        .then()
        .statusCode(403);
    as(adminToken)
        .multiPart("file", "picture", ImageTestData.png(100), "image/png")
        .put(base + "/activities/" + activity + "/image")
        .then()
        .statusCode(403);
    as(adminToken).delete(base + "/activities/" + activity + "/image").then().statusCode(403);
    as(adminToken).delete(base + "/activities/" + activity).then().statusCode(403);

    as(anaToken).get(base).then().body("name", equalTo("Lisbon")).body("coverImageId", nullValue());
    as(anaToken).get(base + "/activities").then().body("content.name", contains("Museum"));
    assertThat(imageRepository.count()).isZero();
  }

  // --- delete ---

  @Test
  void theAdministratorDeletesAnotherUsersTripWithItsActivitiesAndImages() {
    long trip = createTrip(anaToken, "Lisbon", "Portugal");
    long kept = createTrip(anaToken, "Rome", "Italy");
    long activity = createActivity(anaToken, trip, "Museum");
    long keptActivity = createActivity(anaToken, kept, "Forum");
    as(anaToken)
        .multiPart("file", "picture", ImageTestData.png(100), "image/png")
        .put(TRIPS + "/" + trip + "/cover")
        .then()
        .statusCode(201);
    as(anaToken)
        .multiPart("file", "picture", ImageTestData.png(100), "image/png")
        .put(TRIPS + "/" + trip + "/activities/" + activity + "/image")
        .then()
        .statusCode(201);
    assertThat(imageRepository.count()).isEqualTo(2);

    as(adminToken).delete(TRIPS + "/" + trip).then().statusCode(204);

    as(anaToken).get(TRIPS + "/" + trip).then().statusCode(404);
    as(adminToken).get(TRIPS + "/" + trip).then().statusCode(404);
    as(adminToken).delete(TRIPS + "/" + trip).then().statusCode(404);
    assertThat(tripRepository.existsById(trip)).isFalse();
    assertThat(imageRepository.count()).isZero();
    assertThat(activityRepository.count()).isEqualTo(1);
    as(anaToken).get(TRIPS + "/" + kept + "/activities/" + keptActivity).then().statusCode(200);
    as(adminToken)
        .queryParam("search", "lisbon")
        .get(ADMIN_TRIPS)
        .then()
        .body("content", hasSize(0));
  }
}
