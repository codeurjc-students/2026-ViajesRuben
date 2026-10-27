package com.ruben.backend.activity.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
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

@Tag("B6")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ConflictApiE2ETest {

  private static final String PASSWORD = "secret-pass";
  private static final String TRIPS = "/api/v1/trips";
  private static final String TRIP_BODY =
      "{\"name\":\"Rome\",\"destination\":\"Rome\",\"startDate\":\"2027-07-10\","
          + "\"endDate\":\"2027-07-14\",\"budget\":100}";

  @LocalServerPort private int port;

  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AdminAccountInitializer adminAccountInitializer;

  private String anaToken;
  private String bobToken;
  private String adminToken;
  private long tripId;
  private String conflicts;

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
            .body(TRIP_BODY)
            .post(TRIPS)
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
    conflicts = TRIPS + "/" + tripId + "/conflicts";
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

  private long createActivity(String name, String date, String start, String end, String cost) {
    String body =
        "{\"name\":\"%s\",\"date\":\"%s\",\"startTime\":%s,\"endTime\":%s,\"cost\":%s}"
            .formatted(
                name,
                date,
                start == null ? "null" : "\"" + start + "\"",
                end == null ? "null" : "\"" + end + "\"",
                cost == null ? "null" : cost);
    return json(anaToken)
        .body(body)
        .post(TRIPS + "/" + tripId + "/activities")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  // --- permissions ---

  @Test
  void anonymousIsRefusedWith401() {
    given().get(conflicts).then().statusCode(401);
  }

  @Test
  void anotherUserAndTheAdministratorAreRefusedWith403() {
    as(bobToken).get(conflicts).then().statusCode(403);
    as(adminToken).get(conflicts).then().statusCode(403);
  }

  @Test
  void anUnknownTripIsNotFoundForAnyone() {
    as(anaToken).get(TRIPS + "/999999/conflicts").then().statusCode(404);
    as(bobToken).get(TRIPS + "/999999/conflicts").then().statusCode(404);
  }

  @Test
  void aTripIdThatIsNotANumberIsABadRequest() {
    as(anaToken).get(TRIPS + "/abc/conflicts").then().statusCode(400);
  }

  // --- content ---

  @Test
  void aTripWithoutConflictsAnswers200WithEmptyLists() {
    createActivity("Tour", "2027-07-10", "10:00", "11:00", "40");
    createActivity("Lunch", "2027-07-10", "11:00", "12:00", "60");

    as(anaToken)
        .get(conflicts)
        .then()
        .statusCode(200)
        .body("overlaps", empty())
        .body("outsideDates", empty())
        .body("budget", nullValue());
  }

  @Test
  void overlappingActivitiesAreReportedWithTheirDataAndTouchingOnesAreNot() {
    long tour = createActivity("Tour", "2027-07-11", "10:00", "12:00", null);
    long lunch = createActivity("Lunch", "2027-07-11", "11:00", "13:00", null);
    createActivity("Siesta", "2027-07-11", "13:00", "14:00", null);

    as(anaToken)
        .get(conflicts)
        .then()
        .statusCode(200)
        .body("overlaps", hasSize(1))
        .body("overlaps[0].date", equalTo("2027-07-11"))
        .body("overlaps[0].firstId", equalTo((int) tour))
        .body("overlaps[0].firstName", equalTo("Tour"))
        .body("overlaps[0].firstStart", equalTo("10:00:00"))
        .body("overlaps[0].firstEnd", equalTo("12:00:00"))
        .body("overlaps[0].secondId", equalTo((int) lunch))
        .body("overlaps[0].secondName", equalTo("Lunch"))
        .body("overlaps[0].secondStart", equalTo("11:00:00"))
        .body("overlaps[0].secondEnd", equalTo("13:00:00"));
  }

  @Test
  void activitiesWithoutTimesAreNeverOverlaps() {
    createActivity("All day", "2027-07-11", null, null, null);
    createActivity("Tour", "2027-07-11", "10:00", "12:00", null);

    as(anaToken).get(conflicts).then().statusCode(200).body("overlaps", empty());
  }

  @Test
  void aTotalAboveTheBudgetIsReportedAndEqualToItIsNot() {
    createActivity("Tour", "2027-07-10", null, null, "60");
    createActivity("Dinner", "2027-07-11", null, null, "40");
    as(anaToken).get(conflicts).then().statusCode(200).body("budget", nullValue());

    createActivity("Souvenirs", "2027-07-12", null, null, "0.01");

    as(anaToken)
        .get(conflicts)
        .then()
        .statusCode(200)
        .body("budget.totalCost", equalTo(100.01f))
        .body("budget.budget", equalTo(100.0f));
  }

  @Test
  void aTripWithoutBudgetHasNoBudgetConflict() {
    long noBudget =
        json(anaToken)
            .body(TRIP_BODY.replace(",\"budget\":100", ""))
            .post(TRIPS)
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
    json(anaToken)
        .body("{\"name\":\"Expensive\",\"date\":\"2027-07-10\",\"cost\":99999}")
        .post(TRIPS + "/" + noBudget + "/activities")
        .then()
        .statusCode(201);

    as(anaToken)
        .get(TRIPS + "/" + noBudget + "/conflicts")
        .then()
        .statusCode(200)
        .body("budget", nullValue());
  }

  @Test
  void shorteningTheTripReportsTheActivitiesOutsideItAndMovingThemClearsThem() {
    long inside = createActivity("Inside", "2027-07-10", null, null, null);
    long outside = createActivity("Outside", "2027-07-14", null, null, null);
    json(anaToken)
        .body(TRIP_BODY.replace("2027-07-14", "2027-07-12"))
        .put(TRIPS + "/" + tripId)
        .then()
        .statusCode(200);

    as(anaToken)
        .get(conflicts)
        .then()
        .statusCode(200)
        .body("outsideDates", hasSize(1))
        .body("outsideDates[0].activityId", equalTo((int) outside))
        .body("outsideDates[0].name", equalTo("Outside"))
        .body("outsideDates[0].date", equalTo("2027-07-14"));

    json(anaToken)
        .body("{\"name\":\"Outside\",\"date\":\"2027-07-12\"}")
        .put(TRIPS + "/" + tripId + "/activities/" + outside)
        .then()
        .statusCode(200);
    as(anaToken).get(conflicts).then().body("outsideDates", empty());
    as(anaToken).get(TRIPS + "/" + tripId + "/activities/" + inside).then().statusCode(200);
  }

  @Test
  void aConflictNeverBlocksSavingAnActivity() {
    createActivity("Tour", "2027-07-10", "10:00", "12:00", "500");

    json(anaToken)
        .body(
            "{\"name\":\"Lunch\",\"date\":\"2027-07-10\",\"startTime\":\"11:00\","
                + "\"endTime\":\"13:00\",\"cost\":500}")
        .post(TRIPS + "/" + tripId + "/activities")
        .then()
        .statusCode(201);

    as(anaToken)
        .get(conflicts)
        .then()
        .body("overlaps", hasSize(1))
        .body("budget.totalCost", equalTo(1000.0f));
  }
}
