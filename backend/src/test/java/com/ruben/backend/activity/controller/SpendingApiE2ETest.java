package com.ruben.backend.activity.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
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

/** I5: the data of the spending charts, over HTTPS, with who may read it. */
@Tag("I5")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class SpendingApiE2ETest {

  private static final String PASSWORD = "secret-pass";
  private static final String TRIPS = "/api/v1/trips";
  private static final String TRIP_BODY =
      "{\"name\":\"Rome\",\"destination\":\"Rome\",\"startDate\":\"2027-07-10\","
          + "\"endDate\":\"2027-07-14\",\"budget\":200}";
  private static final String ACCEPTED = "{\"status\":\"ACCEPTED\"}";

  @LocalServerPort private int port;

  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AdminAccountInitializer adminAccountInitializer;

  private String anaToken;
  private String bobToken;
  private String carlaToken;
  private String adminToken;
  private long tripId;
  private String activities;
  private String spending;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "https://localhost";
    RestAssured.port = port;
    RestAssured.useRelaxedHTTPSValidation();
    databaseCleaner.clear();
    adminAccountInitializer.run(new DefaultApplicationArguments());
    newUser("Ana", "ana@example.com");
    newUser("Bob", "bob@example.com");
    newUser("Carla", "carla@example.com");
    anaToken = tokenOf("ana@example.com", PASSWORD);
    bobToken = tokenOf("bob@example.com", PASSWORD);
    carlaToken = tokenOf("carla@example.com", PASSWORD);
    adminToken = tokenOf("admin@test.tripmate.com", "Admin-test-1");
    tripId = createTrip(TRIP_BODY);
    activities = TRIPS + "/" + tripId + "/activities";
    spending = activities + "/spending";
    join(bobToken, "bob@example.com");
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

  private long createTrip(String body) {
    return json(anaToken)
        .body(body)
        .post(TRIPS)
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
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

  private void addActivity(String token, String url, String date, String cost, String category) {
    String costPart = cost == null ? "" : ",\"cost\":" + cost;
    String categoryPart = category == null ? "" : ",\"category\":\"" + category + "\"";
    json(token)
        .body("{\"name\":\"Activity\",\"date\":\"%s\"%s%s}".formatted(date, costPart, categoryPart))
        .post(url)
        .then()
        .statusCode(201);
  }

  // --- who may read it ---

  @Test
  void theCreatorTheParticipantAndTheAdministratorReadTheSpending() {
    as(anaToken).get(spending).then().statusCode(200);
    as(bobToken).get(spending).then().statusCode(200);
    as(adminToken).get(spending).then().statusCode(200);
  }

  @Test
  void aStrangerGets403TheAnonymousGets401AndAMissingTripGets404() {
    as(carlaToken).get(spending).then().statusCode(403);
    given().get(spending).then().statusCode(401);
    as(anaToken).get(TRIPS + "/999999/activities/spending").then().statusCode(404);
    as(carlaToken).get(TRIPS + "/999999/activities/spending").then().statusCode(404);
    as(anaToken).get(TRIPS + "/abc/activities/spending").then().statusCode(400);
  }

  // --- what it says ---

  @Test
  void aTripWithoutCostsHasZeroEveryDayAndNoCategories() {
    as(anaToken)
        .get(spending)
        .then()
        .statusCode(200)
        .body("budget", equalTo(200f))
        .body("totalCost", equalTo(0))
        .body("byCategory", empty())
        .body("byDay", hasSize(5))
        .body(
            "byDay.date",
            contains("2027-07-10", "2027-07-11", "2027-07-12", "2027-07-13", "2027-07-14"))
        .body("byDay.cumulative", contains(0, 0, 0, 0, 0));
  }

  @Test
  void theSumsPerCategoryAndPerDayIgnoreActivitiesWithoutCostAndFillTheDaysWithZero() {
    addActivity(anaToken, activities, "2027-07-10", "20.00", "FOOD");
    addActivity(bobToken, activities, "2027-07-10", "15.50", "FOOD");
    addActivity(anaToken, activities, "2027-07-12", "100.00", "ACCOMMODATION");
    addActivity(bobToken, activities, "2027-07-12", "4.50", null);
    addActivity(anaToken, activities, "2027-07-13", null, "TRANSPORT");

    as(bobToken)
        .get(spending)
        .then()
        .statusCode(200)
        .body("budget", equalTo(200f))
        .body("totalCost", equalTo(140f))
        .body("byCategory.category", contains("ACCOMMODATION", "FOOD", null))
        .body("byCategory.total", contains(100f, 35.5f, 4.5f))
        .body("byDay.total", contains(35.5f, 0, 104.5f, 0, 0))
        .body("byDay.cumulative", contains(35.5f, 35.5f, 140f, 140f, 140f));
  }

  @Test
  void aTripWithoutBudgetHasANullBudget() {
    long noBudget =
        createTrip(
            "{\"name\":\"Rome\",\"destination\":\"Rome\",\"startDate\":\"2027-07-10\","
                + "\"endDate\":\"2027-07-10\"}");
    String url = TRIPS + "/" + noBudget + "/activities";
    addActivity(anaToken, url, "2027-07-10", "9.99", "LEISURE");

    as(anaToken)
        .get(url + "/spending")
        .then()
        .statusCode(200)
        .body("budget", nullValue())
        .body("totalCost", equalTo(9.99f))
        .body("byDay", hasSize(1))
        .body("byDay[0].cumulative", equalTo(9.99f));
  }
}
