package com.ruben.backend.activity.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.activity.repository.ActivityLogEntryRepository;
import com.ruben.backend.activity.repository.ActivityRepository;
import com.ruben.backend.image.ImageTestData;
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

/**
 * I4: the creator and the participants plan the itinerary together, and every change is written to
 * the trip's change log.
 */
@Tag("I4")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class CollaborativeItineraryApiE2ETest {

  private static final String PASSWORD = "secret-pass";
  private static final String TRIPS = "/api/v1/trips";
  private static final String TRIP_BODY =
      "{\"name\":\"Rome\",\"destination\":\"Rome\",\"startDate\":\"2027-07-10\","
          + "\"endDate\":\"2027-07-14\"}";
  private static final String ACCEPTED = "{\"status\":\"ACCEPTED\"}";

  @LocalServerPort private int port;

  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private UserRepository userRepository;
  @Autowired private ActivityRepository activityRepository;
  @Autowired private ActivityLogEntryRepository entryRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AdminAccountInitializer adminAccountInitializer;

  private String anaToken;
  private String bobToken;
  private String carlaToken;
  private String adminToken;
  private long anaId;
  private long bobId;
  private long tripId;
  private String activities;
  private String log;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "https://localhost";
    RestAssured.port = port;
    RestAssured.useRelaxedHTTPSValidation();
    databaseCleaner.clear();
    adminAccountInitializer.run(new DefaultApplicationArguments());
    anaId = newUser("Ana", "ana@example.com");
    bobId = newUser("Bob", "bob@example.com");
    newUser("Carla", "carla@example.com");
    anaToken = tokenOf("ana@example.com", PASSWORD);
    bobToken = tokenOf("bob@example.com", PASSWORD);
    carlaToken = tokenOf("carla@example.com", PASSWORD);
    adminToken = tokenOf("admin@test.tripmate.com", "Admin-test-1");
    tripId =
        json(anaToken)
            .body(TRIP_BODY)
            .post(TRIPS)
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
    activities = TRIPS + "/" + tripId + "/activities";
    log = TRIPS + "/" + tripId + "/activity-log";
    join(bobToken, "bob@example.com");
  }

  @AfterEach
  void tearDown() {
    RestAssured.reset();
  }

  // --- helpers ---

  private long newUser(String name, String email) {
    return userRepository
        .save(new User(name, email, passwordEncoder.encode(PASSWORD), Role.USER))
        .getId();
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

  private void join(String token, String email) {
    long id =
        json(anaToken)
            .body("{\"email\":\"%s\"}".formatted(email))
            .post(TRIPS + "/" + tripId + "/invitations")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
    json(token).body(ACCEPTED).put("/api/v1/invitations/" + id + "/status").then().statusCode(200);
  }

  private static String body(String name, String date) {
    return "{\"name\":\"%s\",\"date\":\"%s\"}".formatted(name, date);
  }

  private static String bodyWithCost(String name, String date, String cost) {
    return "{\"name\":\"%s\",\"date\":\"%s\",\"cost\":%s}".formatted(name, date, cost);
  }

  private long create(String token, String name) {
    return json(token)
        .body(body(name, "2027-07-11"))
        .post(activities)
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  // --- who may change the itinerary ---

  @Test
  void aParticipantCreatesEditsAndDeletesActivitiesOfTheCreator() {
    long museum = create(anaToken, "Museum");

    json(bobToken)
        .body(body("Walk", "2027-07-12"))
        .post(activities)
        .then()
        .statusCode(201)
        .body("authorId", equalTo((int) bobId))
        .body("authorName", equalTo("Bob"));
    json(bobToken)
        .body(body("Louvre", "2027-07-11"))
        .put(activities + "/" + museum)
        .then()
        .statusCode(200)
        .body("name", equalTo("Louvre"))
        .body("authorId", equalTo((int) anaId))
        .body("authorName", equalTo("Ana"));
    as(bobToken).delete(activities + "/" + museum).then().statusCode(204);

    as(anaToken).get(activities + "/" + museum).then().statusCode(404);
    assertThat(activityRepository.count()).isEqualTo(1);
  }

  @Test
  void theCreatorEditsAndDeletesActivitiesOfAParticipant() {
    long walk = create(bobToken, "Walk");

    json(anaToken)
        .body(body("Long walk", "2027-07-11"))
        .put(activities + "/" + walk)
        .then()
        .statusCode(200)
        .body("name", equalTo("Long walk"))
        .body("authorName", equalTo("Bob"));
    as(anaToken).delete(activities + "/" + walk).then().statusCode(204);

    assertThat(activityRepository.count()).isZero();
  }

  @Test
  void aParticipantChangesThePictureOfAnActivityOfTheCreator() {
    long museum = create(anaToken, "Museum");
    String item = activities + "/" + museum;

    as(bobToken)
        .multiPart("file", "picture", ImageTestData.png(100), "image/png")
        .put(item + "/image")
        .then()
        .statusCode(201);
    as(bobToken)
        .multiPart("file", "picture", ImageTestData.png(120), "image/png")
        .put(item + "/image")
        .then()
        .statusCode(200);
    as(bobToken).delete(item + "/image").then().statusCode(204);

    as(anaToken)
        .get(log)
        .then()
        .statusCode(200)
        .body("content.action", contains("UPDATED", "UPDATED", "UPDATED", "CREATED"))
        .body("content[0].changedFields", contains("image"))
        .body("content[0].userName", equalTo("Bob"));
  }

  @Test
  void theAdministratorAndStrangersGet403OnEveryWriteAndNothingChanges() {
    long museum = create(anaToken, "Museum");
    String item = activities + "/" + museum;
    long lines = entryRepository.count();

    for (String token : new String[] {adminToken, carlaToken}) {
      json(token).body(body("Mine", "2027-07-10")).post(activities).then().statusCode(403);
      json(token).body(body("Mine", "2027-07-10")).put(item).then().statusCode(403);
      as(token).delete(item).then().statusCode(403);
      as(token)
          .multiPart("file", "picture", ImageTestData.png(100), "image/png")
          .put(item + "/image")
          .then()
          .statusCode(403);
      as(token).delete(item + "/image").then().statusCode(403);
    }

    as(anaToken).get(item).then().statusCode(200).body("name", equalTo("Museum"));
    assertThat(activityRepository.count()).isEqualTo(1);
    assertThat(entryRepository.count()).isEqualTo(lines);
  }

  @Test
  void anonymousUsersGet401OnEveryWriteAndOnTheLog() {
    long museum = create(anaToken, "Museum");
    String item = activities + "/" + museum;

    given()
        .contentType(ContentType.JSON)
        .body(body("Mine", "2027-07-10"))
        .post(activities)
        .then()
        .statusCode(401);
    given()
        .contentType(ContentType.JSON)
        .body(body("Mine", "2027-07-10"))
        .put(item)
        .then()
        .statusCode(401);
    given().delete(item).then().statusCode(401);
    given()
        .multiPart("file", "picture", ImageTestData.png(100), "image/png")
        .put(item + "/image")
        .then()
        .statusCode(401);
    given().delete(item + "/image").then().statusCode(401);
    given().get(log).then().statusCode(401);
  }

  // --- the change log ---

  @Test
  void everyChangeAppearsOnceInTheLogWithItsActionUserAndChangedFields() {
    long museum = create(anaToken, "Museum");
    json(bobToken)
        .body(bodyWithCost("Louvre", "2027-07-11", "20.00"))
        .put(activities + "/" + museum)
        .then()
        .statusCode(200);
    as(bobToken).delete(activities + "/" + museum).then().statusCode(204);

    as(anaToken)
        .get(log)
        .then()
        .statusCode(200)
        .body("totalElements", equalTo(3))
        .body("content", hasSize(3))
        .body("content[0].action", equalTo("DELETED"))
        .body("content[0].userName", equalTo("Bob"))
        .body("content[0].userId", equalTo((int) bobId))
        .body("content[0].activityName", equalTo("Louvre"))
        .body("content[0].activityId", nullValue())
        .body("content[0].changedFields", empty())
        .body("content[0].createdAt", notNullValue())
        .body("content[1].action", equalTo("UPDATED"))
        .body("content[1].userName", equalTo("Bob"))
        .body("content[1].changedFields", contains("name", "cost"))
        .body("content[1].activityId", nullValue())
        .body("content[2].action", equalTo("CREATED"))
        .body("content[2].userName", equalTo("Ana"))
        .body("content[2].activityName", equalTo("Museum"))
        .body("content[2].changedFields", empty());
  }

  @Test
  void aChangeThatFailsWritesNoLine() {
    long museum = create(anaToken, "Museum");
    long lines = entryRepository.count();

    json(bobToken).body(body("Late", "2027-07-15")).post(activities).then().statusCode(400);
    json(bobToken).body("{\"name\":\"\"}").put(activities + "/" + museum).then().statusCode(400);
    json(bobToken)
        .body(body("Ghost", "2027-07-11"))
        .put(activities + "/999999")
        .then()
        .statusCode(404);
    as(bobToken).delete(activities + "/999999").then().statusCode(404);
    as(bobToken)
        .multiPart("file", "picture", new byte[0], "image/png")
        .put(activities + "/" + museum + "/image")
        .then()
        .statusCode(400);

    assertThat(entryRepository.count()).isEqualTo(lines);
  }

  @Test
  void theLogIsPagedNewestFirst() {
    for (int i = 1; i <= 12; i++) {
      create(i % 2 == 0 ? bobToken : anaToken, "Activity " + i);
    }

    as(anaToken)
        .get(log)
        .then()
        .statusCode(200)
        .body("size", equalTo(10))
        .body("totalElements", equalTo(12))
        .body("last", equalTo(false))
        .body("content", hasSize(10))
        .body("content[0].activityName", equalTo("Activity 12"))
        .body("content[0].userName", equalTo("Bob"))
        .body("content[9].activityName", equalTo("Activity 3"));
    as(anaToken)
        .queryParam("page", 1)
        .get(log)
        .then()
        .statusCode(200)
        .body("content", hasSize(2))
        .body("last", equalTo(true))
        .body("content[1].activityName", equalTo("Activity 1"));
  }

  @Test
  void theCreatorTheParticipantsAndTheAdministratorReadTheLogButAStrangerDoesNot() {
    create(anaToken, "Museum");

    as(anaToken).get(log).then().statusCode(200).body("content", hasSize(1));
    as(bobToken).get(log).then().statusCode(200).body("content", hasSize(1));
    as(adminToken).get(log).then().statusCode(200).body("content", hasSize(1));
    as(carlaToken).get(log).then().statusCode(403);
  }

  @Test
  void theLogOfAMissingTripIsNotFoundAndASortByAnotherPropertyIsRejected() {
    as(anaToken).get(TRIPS + "/999999/activity-log").then().statusCode(404);
    as(anaToken).get(TRIPS + "/abc/activity-log").then().statusCode(400);
    as(anaToken).queryParam("sort", "user.password").get(log).then().statusCode(400);
    as(anaToken).queryParam("sort", "userName").get(log).then().statusCode(400);
  }

  @Test
  void deletingTheTripDeletesItsLog() {
    long museum = create(anaToken, "Museum");
    json(bobToken)
        .body(body("Louvre", "2027-07-11"))
        .put(activities + "/" + museum)
        .then()
        .statusCode(200);
    assertThat(entryRepository.count()).isEqualTo(2);

    as(anaToken).delete(TRIPS + "/" + tripId).then().statusCode(204);

    assertThat(entryRepository.count()).isZero();
    assertThat(activityRepository.count()).isZero();
  }

  @Test
  void aParticipantWhoLeavesKeepsTheirActivitiesAndLogLinesButLosesAccess() {
    create(bobToken, "Walk");

    as(bobToken).delete(TRIPS + "/" + tripId + "/participants/" + bobId).then().statusCode(204);

    as(anaToken)
        .get(activities)
        .then()
        .statusCode(200)
        .body("content", hasSize(1))
        .body("content[0].authorName", equalTo("Bob"));
    as(anaToken)
        .get(log)
        .then()
        .statusCode(200)
        .body("content", hasSize(1))
        .body("content[0].userName", equalTo("Bob"));
    as(bobToken).get(log).then().statusCode(403);
    json(bobToken).body(body("Again", "2027-07-11")).post(activities).then().statusCode(403);
  }

  @Test
  void aPublicItineraryNeverNamesWhoAddedTheActivities() {
    create(bobToken, "Walk");
    json(anaToken)
        .body("{\"published\":true}")
        .put(TRIPS + "/" + tripId + "/publication")
        .then()
        .statusCode(200);

    given()
        .get(TRIPS + "/public/" + tripId + "/activities")
        .then()
        .statusCode(200)
        .body("content", hasSize(1))
        .body("content[0].name", equalTo("Walk"))
        .body("content[0].authorName", nullValue());
  }
}
