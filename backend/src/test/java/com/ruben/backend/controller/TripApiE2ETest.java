package com.ruben.backend.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;

import com.ruben.backend.TestcontainersConfiguration;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class TripApiE2ETest {

  @LocalServerPort private int port;

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
  }

  @Test
  void getTripsReturnsSampleTrips() {
    given()
        .when()
        .get("/api/trips")
        .then()
        .statusCode(200)
        .body("name", hasItems("Trip 1", "Trip 2", "Trip 3"))
        .body("destination", hasItems("Lisbon", "Rome", "Tokyo"));
  }
}
