package com.ruben.backend.trip.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.common.security.AdminProperties;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AdminAccountInitializer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class TripApiE2ETest {

  @LocalServerPort private int port;

  @Autowired private TripRepository tripRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private AdminAccountInitializer adminAccountInitializer;
  @Autowired private AdminProperties adminProperties;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "https://localhost";
    RestAssured.port = port;
    RestAssured.useRelaxedHTTPSValidation();
  }

  @AfterEach
  void tearDown() {
    RestAssured.reset();
  }

  @Test
  void getTripsReturnsSampleTrips() {
    given()
        .when()
        .get("/api/v1/trips")
        .then()
        .statusCode(200)
        .body("name", hasItems("Trip 1", "Trip 2", "Trip 3"))
        .body("destination", hasItems("Lisbon", "Rome", "Tokyo"));
  }

  @Test
  void getTripByIdReturnsTheTripForALoggedInUser() {
    userRepository.deleteAll();
    adminAccountInitializer.run(new DefaultApplicationArguments());
    Trip trip =
        tripRepository.save(
            new Trip("By id", "Oslo", LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 4)));
    String token =
        given()
            .contentType(ContentType.JSON)
            .body(
                "{\"email\":\"%s\",\"password\":\"Admin-test-1\"}"
                    .formatted(adminProperties.email()))
            .post("/api/v1/auth/login")
            .then()
            .statusCode(200)
            .extract()
            .cookie("AuthToken");

    given()
        .cookie("AuthToken", token)
        .get("/api/v1/trips/" + trip.getId())
        .then()
        .statusCode(200)
        .body("id", equalTo(trip.getId().intValue()))
        .body("destination", equalTo("Oslo"));
  }

  @Test
  void getTripByIdWithoutLoggingInReturns401() {
    given().get("/api/v1/trips/1").then().statusCode(401);
  }
}
