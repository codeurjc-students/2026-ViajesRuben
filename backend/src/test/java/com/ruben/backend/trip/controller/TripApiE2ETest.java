package com.ruben.backend.trip.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AdminAccountInitializer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

@Tag("B4")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class TripApiE2ETest {

  private static final String PASSWORD = "secret-pass";

  @LocalServerPort private int port;

  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private TripRepository tripRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AdminAccountInitializer adminAccountInitializer;

  private User ana;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "https://localhost";
    RestAssured.port = port;
    RestAssured.useRelaxedHTTPSValidation();
    databaseCleaner.clear();
    adminAccountInitializer.run(new DefaultApplicationArguments());
    ana =
        userRepository.save(
            new User("Ana", "ana@example.com", passwordEncoder.encode(PASSWORD), Role.USER));
  }

  @AfterEach
  void tearDown() {
    RestAssured.reset();
  }

  private String tokenOf(String email) {
    return given()
        .contentType(ContentType.JSON)
        .body("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD))
        .post("/api/v1/auth/login")
        .then()
        .statusCode(200)
        .extract()
        .cookie("AuthToken");
  }

  private List<Trip> seedTrips(User creator, int count) {
    List<Trip> trips = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
      trips.add(
          new Trip(
              creator,
              "Trip " + i,
              "City " + i,
              LocalDate.of(2027, 1, 1).plusDays(i),
              LocalDate.of(2027, 1, 3).plusDays(i)));
    }
    return tripRepository.saveAll(trips);
  }

  @Test
  void getTripsReturnsTheFirstTenOfTheUsersTripsByDefault() {
    seedTrips(ana, 12);

    given()
        .cookie("AuthToken", tokenOf("ana@example.com"))
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
    seedTrips(ana, 12);

    given()
        .cookie("AuthToken", tokenOf("ana@example.com"))
        .queryParam("page", 1)
        .get("/api/v1/trips")
        .then()
        .statusCode(200)
        .body("content.name", contains("Trip 11", "Trip 12"))
        .body("page", equalTo(1))
        .body("last", equalTo(true));
  }

  @Test
  void getTripsNeverReturnsAnotherUsersTrips() {
    seedTrips(ana, 2);
    User bob =
        userRepository.save(
            new User("Bob", "bob@example.com", passwordEncoder.encode(PASSWORD), Role.USER));
    seedTrips(bob, 5);

    given()
        .cookie("AuthToken", tokenOf("ana@example.com"))
        .get("/api/v1/trips")
        .then()
        .statusCode(200)
        .body("totalElements", equalTo(2))
        .body("content.creatorId", contains(ana.getId().intValue(), ana.getId().intValue()));
  }

  @Test
  void getTripsHonoursTheSizeParameterUpToTheMaximum() {
    seedTrips(ana, 12);
    String token = tokenOf("ana@example.com");

    given()
        .cookie("AuthToken", token)
        .queryParam("size", 5)
        .get("/api/v1/trips")
        .then()
        .statusCode(200)
        .body("content", hasSize(5))
        .body("size", equalTo(5));
    given()
        .cookie("AuthToken", token)
        .queryParam("size", 1000)
        .get("/api/v1/trips")
        .then()
        .statusCode(200)
        .body("size", equalTo(50))
        .body("content", hasSize(12));
  }

  @Test
  void getTripsWithAPageBeyondTheEndReturnsAnEmptyLastPage() {
    seedTrips(ana, 3);

    given()
        .cookie("AuthToken", tokenOf("ana@example.com"))
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
    seedTrips(ana, 1);

    given()
        .cookie("AuthToken", tokenOf("ana@example.com"))
        .queryParam("sort", "bogus")
        .get("/api/v1/trips")
        .then()
        .statusCode(400)
        .body("message", equalTo("Cannot sort by 'bogus'"));
  }

  @Test
  void getTripsWithoutLoggingInReturns401() {
    given().get("/api/v1/trips").then().statusCode(401);
  }

  @Test
  void getTripByIdReturnsTheTripToItsCreator() {
    Trip trip = seedTrips(ana, 1).get(0);

    given()
        .cookie("AuthToken", tokenOf("ana@example.com"))
        .get("/api/v1/trips/" + trip.getId())
        .then()
        .statusCode(200)
        .body("id", equalTo(trip.getId().intValue()))
        .body("destination", equalTo("City 1"));
  }

  @Test
  void getTripByIdWithoutLoggingInReturns401() {
    given().get("/api/v1/trips/1").then().statusCode(401);
  }
}
