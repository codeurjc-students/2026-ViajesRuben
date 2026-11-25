package com.ruben.backend.metrics.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AdminAccountInitializer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
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

@Tag("I6")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AdminMetricsApiE2ETest {

  private static final String PASSWORD = "secret-pass";
  private static final String METRICS = "/api/v1/admin/metrics";
  private static final String TRIPS = "/api/v1/trips";

  @LocalServerPort private int port;

  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AdminAccountInitializer adminAccountInitializer;

  private String anaToken;
  private String adminToken;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "https://localhost";
    RestAssured.port = port;
    RestAssured.useRelaxedHTTPSValidation();
    databaseCleaner.clear();
    adminAccountInitializer.run(new DefaultApplicationArguments());
    userRepository.save(
        new User("Ana", "ana@example.com", passwordEncoder.encode(PASSWORD), Role.USER));
    anaToken = tokenOf("ana@example.com", PASSWORD);
    adminToken = tokenOf("admin@test.tripmate.com", "Admin-test-1");
  }

  @AfterEach
  void tearDown() {
    RestAssured.reset();
  }

  // --- helpers ---

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

  private long metric(String name) {
    JsonPath body = as(adminToken).get(METRICS).then().statusCode(200).extract().jsonPath();
    return body.getLong(name);
  }

  private static long createTrip(String token, String name) {
    return json(token)
        .body(
            ("{\"name\":\"%s\",\"destination\":\"City\",\"startDate\":\"2027-07-10\","
                    + "\"endDate\":\"2027-07-14\"}")
                .formatted(name))
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

  // --- access ---

  @Test
  void theMetricsRequireLoginAndTheAdministratorRole() {
    given().get(METRICS).then().statusCode(401).body("status", equalTo(401));
    as(anaToken).get(METRICS).then().statusCode(403).body("status", equalTo(403));
    as(adminToken).get(METRICS).then().statusCode(200);
  }

  // --- totals ---

  @Test
  void theAdministratorGetsTheThreeTotalsOfKnownData() {
    // A blocked user is still an account; an unpublished trip is still a trip.
    User bob = new User("Bob", "bob@example.com", passwordEncoder.encode(PASSWORD), Role.USER);
    bob.setStatus(UserStatus.BLOCKED);
    userRepository.save(bob);
    long rome = createTrip(anaToken, "Rome");
    createTrip(anaToken, "Oslo");
    json(anaToken)
        .body("{\"published\":true}")
        .put(TRIPS + "/" + rome + "/publication")
        .then()
        .statusCode(200);
    createActivity(anaToken, rome, "Colosseum");

    as(adminToken)
        .get(METRICS)
        .then()
        .statusCode(200)
        .body("users", equalTo(2))
        .body("trips", equalTo(2))
        .body("activities", equalTo(1))
        .body("$", not(hasKey("aiRequests")));
  }

  @Test
  void theTotalsFollowRegisteringCreatingAndDeleting() {
    long users = metric("users");
    long trips = metric("trips");
    long activities = metric("activities");

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Bob\",\"email\":\"bob@example.com\",\"password\":\"secret-pass\"}")
        .post("/api/v1/users")
        .then()
        .statusCode(201);
    assertThat(metric("users")).isEqualTo(users + 1);

    long tripId = createTrip(anaToken, "Rome");
    assertThat(metric("trips")).isEqualTo(trips + 1);

    long activityId = createActivity(anaToken, tripId, "Colosseum");
    assertThat(metric("activities")).isEqualTo(activities + 1);

    as(anaToken).delete(TRIPS + "/" + tripId + "/activities/" + activityId).then().statusCode(204);
    assertThat(metric("activities")).isEqualTo(activities);

    as(anaToken).delete(TRIPS + "/" + tripId).then().statusCode(204);
    assertThat(metric("trips")).isEqualTo(trips);
  }
}
