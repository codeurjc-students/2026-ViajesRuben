package com.ruben.backend.trip.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.common.security.AdminProperties;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AdminAccountInitializer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

  private void seedTrips(int count) {
    tripRepository.deleteAll();
    List<Trip> trips = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
      trips.add(
          new Trip(
              "Trip " + i,
              "City " + i,
              LocalDate.of(2027, 1, 1).plusDays(i),
              LocalDate.of(2027, 1, 3).plusDays(i)));
    }
    tripRepository.saveAll(trips);
  }

  @Test
  void getTripsReturnsTheFirstTenTripsByDefault() {
    seedTrips(12);

    given()
        .get("/api/v1/trips")
        .then()
        .statusCode(200)
        .body("content", hasSize(10))
        .body("content[0].name", equalTo("Trip 1"))
        .body("content[9].name", equalTo("Trip 10"))
        .body("page", equalTo(0))
        .body("size", equalTo(10))
        .body("totalElements", equalTo(12))
        .body("last", equalTo(false));
  }

  @Test
  void getTripsReturnsTheRestOnTheSecondPage() {
    seedTrips(12);

    given()
        .queryParam("page", 1)
        .get("/api/v1/trips")
        .then()
        .statusCode(200)
        .body("content.name", contains("Trip 11", "Trip 12"))
        .body("page", equalTo(1))
        .body("last", equalTo(true));
  }

  @Test
  void getTripsHonoursTheSizeParameterUpToTheMaximum() {
    seedTrips(12);

    given()
        .queryParam("size", 5)
        .get("/api/v1/trips")
        .then()
        .statusCode(200)
        .body("content", hasSize(5))
        .body("size", equalTo(5));
    given()
        .queryParam("size", 1000)
        .get("/api/v1/trips")
        .then()
        .statusCode(200)
        .body("size", equalTo(50))
        .body("content", hasSize(12));
  }

  @Test
  void getTripsWithAPageBeyondTheEndReturnsAnEmptyLastPage() {
    seedTrips(3);

    given()
        .queryParam("page", 5)
        .get("/api/v1/trips")
        .then()
        .statusCode(200)
        .body("content", hasSize(0))
        .body("totalElements", equalTo(3))
        .body("last", equalTo(true));
  }

  @Test
  void getTripsSortingByAnUnknownPropertyReturns400() {
    seedTrips(1);

    given()
        .queryParam("sort", "bogus")
        .get("/api/v1/trips")
        .then()
        .statusCode(400)
        .body("message", equalTo("Cannot sort by 'bogus'"));
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
