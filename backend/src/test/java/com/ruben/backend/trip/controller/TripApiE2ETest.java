package com.ruben.backend.trip.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.image.repository.ImageRepository;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AdminAccountInitializer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
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
@Tag("B5")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class TripApiE2ETest {

  private static final String PASSWORD = "secret-pass";
  private static final String TRIPS = "/api/v1/trips";
  private static final String VALID_TRIP =
      "{\"name\":\"Lisbon trip\",\"destination\":\"Lisbon\",\"startDate\":\"2027-05-01\","
          + "\"endDate\":\"2027-05-05\",\"budget\":1200.5,\"description\":\"Pastéis\"}";

  @LocalServerPort private int port;

  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private TripRepository tripRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private ImageRepository imageRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AdminAccountInitializer adminAccountInitializer;

  private User ana;
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
    ana = newUser("Ana", "ana@example.com");
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

  private User newUser(String name, String email) {
    return userRepository.save(new User(name, email, passwordEncoder.encode(PASSWORD), Role.USER));
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

  /** Creates a trip through the API as the given user and returns its id. */
  private long createTrip(String token) {
    return json(token)
        .body(VALID_TRIP)
        .post(TRIPS)
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private static Response uploadCover(String token, long id, byte[] data, String type) {
    return as(token).multiPart("file", "picture", data, type).put(TRIPS + "/" + id + "/cover");
  }

  private static String coverImageId(String token, long id) {
    return as(token).get(TRIPS + "/" + id).then().statusCode(200).extract().path("coverImageId");
  }

  // --- not logged in ---

  @Test
  void everyTripOperationRequiresLogin() {
    String trip = TRIPS + "/1";

    given().get(TRIPS).then().statusCode(401);
    given().contentType(ContentType.JSON).body(VALID_TRIP).post(TRIPS).then().statusCode(401);
    given().get(trip).then().statusCode(401);
    given().contentType(ContentType.JSON).body(VALID_TRIP).put(trip).then().statusCode(401);
    given().delete(trip).then().statusCode(401);
    given()
        .multiPart("file", "picture", ImageTestData.png(10), "image/png")
        .put(trip + "/cover")
        .then()
        .statusCode(401);
    given().get(trip + "/cover").then().statusCode(401);
    given().delete(trip + "/cover").then().statusCode(401);
  }

  // --- list ---

  @Test
  void getTripsReturnsTheFirstTenOfTheUsersTripsByDefault() {
    seedTrips(ana, 12);

    as(anaToken)
        .get(TRIPS)
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

    as(anaToken)
        .queryParam("page", 1)
        .get(TRIPS)
        .then()
        .statusCode(200)
        .body("content.name", contains("Trip 11", "Trip 12"))
        .body("page", equalTo(1))
        .body("last", equalTo(true));
  }

  @Test
  void getTripsNeverReturnsAnotherUsersTrips() {
    seedTrips(ana, 2);
    seedTrips(userRepository.findByEmail("bob@example.com").orElseThrow(), 5);

    as(anaToken)
        .get(TRIPS)
        .then()
        .statusCode(200)
        .body("totalElements", equalTo(2))
        .body("content.creatorId", contains(ana.getId().intValue(), ana.getId().intValue()));
    as(adminToken).get(TRIPS).then().statusCode(200).body("totalElements", equalTo(0));
  }

  @Test
  void getTripsHonoursTheSizeParameterUpToTheMaximum() {
    seedTrips(ana, 12);

    as(anaToken)
        .queryParam("size", 5)
        .get(TRIPS)
        .then()
        .statusCode(200)
        .body("content", hasSize(5))
        .body("size", equalTo(5));
    as(anaToken)
        .queryParam("size", 1000)
        .get(TRIPS)
        .then()
        .statusCode(200)
        .body("size", equalTo(50))
        .body("content", hasSize(12));
  }

  @Test
  void getTripsWithAPageBeyondTheEndReturnsAnEmptyLastPage() {
    seedTrips(ana, 3);

    as(anaToken)
        .queryParam("page", 5)
        .get(TRIPS)
        .then()
        .statusCode(200)
        .body("content", hasSize(0))
        .body("totalElements", equalTo(3))
        .body("last", equalTo(true));
  }

  @Test
  void getTripsCanSortDescendingByName() {
    seedTrips(ana, 3);

    as(anaToken)
        .queryParam("sort", "name,desc")
        .get(TRIPS)
        .then()
        .statusCode(200)
        .body("content.name", contains("Trip 3", "Trip 2", "Trip 1"));
  }

  @Test
  void getTripsSortingByAnUnknownOrNestedPropertyReturns400() {
    seedTrips(ana, 1);

    as(anaToken)
        .queryParam("sort", "bogus")
        .get(TRIPS)
        .then()
        .statusCode(400)
        .body("message", equalTo("Cannot sort by 'bogus'"));
    as(anaToken)
        .queryParam("sort", "creator.password")
        .get(TRIPS)
        .then()
        .statusCode(400)
        .body("message", equalTo("Cannot sort by 'creator.password'"));
  }

  // --- create ---

  @Test
  void createTripAnswers201WithLocationAndTheTrip() {
    Response response = json(anaToken).body(VALID_TRIP).post(TRIPS);

    response
        .then()
        .statusCode(201)
        .body("creatorId", equalTo(ana.getId().intValue()))
        .body("name", equalTo("Lisbon trip"))
        .body("destination", equalTo("Lisbon"))
        .body("startDate", equalTo("2027-05-01"))
        .body("endDate", equalTo("2027-05-05"))
        .body("budget", equalTo(1200.5f))
        .body("description", equalTo("Pastéis"))
        .body("coverImageId", nullValue());
    String location = response.header("Location");
    assertThat(location).endsWith(TRIPS + "/" + response.jsonPath().getLong("id"));
    as(anaToken).get(TRIPS + "/" + response.jsonPath().getLong("id")).then().statusCode(200);
  }

  @Test
  void createTripWithoutBudgetOrDescriptionIsValidAndAOneDayTripToo() {
    json(anaToken)
        .body(
            "{\"name\":\"Day\",\"destination\":\"Oslo\",\"startDate\":\"2027-05-01\","
                + "\"endDate\":\"2027-05-01\"}")
        .post(TRIPS)
        .then()
        .statusCode(201)
        .body("budget", nullValue())
        .body("description", nullValue());
  }

  @Test
  void createTripWithInvalidDataReturns400AndStoresNothing() {
    String longName = "n".repeat(101);
    String[] bodies = {
      "{\"destination\":\"Lisbon\",\"startDate\":\"2027-05-01\",\"endDate\":\"2027-05-05\"}",
      "{\"name\":\"  \",\"destination\":\"Lisbon\",\"startDate\":\"2027-05-01\",\"endDate\":\"2027-05-05\"}",
      "{\"name\":\"Trip\",\"startDate\":\"2027-05-01\",\"endDate\":\"2027-05-05\"}",
      "{\"name\":\"Trip\",\"destination\":\"Lisbon\",\"endDate\":\"2027-05-05\"}",
      "{\"name\":\"Trip\",\"destination\":\"Lisbon\",\"startDate\":\"2027-05-01\"}",
      "{\"name\":\"%s\",\"destination\":\"Lisbon\",\"startDate\":\"2027-05-01\",\"endDate\":\"2027-05-05\"}"
          .formatted(longName),
      "{\"name\":\"Trip\",\"destination\":\"Lisbon\",\"startDate\":\"2027-05-01\",\"endDate\":\"2027-05-05\",\"budget\":-1}",
      "{\"name\":\"Trip\",\"destination\":\"Lisbon\",\"startDate\":\"2027-05-01\",\"endDate\":\"2027-05-05\",\"description\":\"%s\"}"
          .formatted("d".repeat(2001)),
    };

    for (String body : bodies) {
      json(anaToken).body(body).post(TRIPS).then().statusCode(400);
    }
    assertThat(tripRepository.count()).isZero();
  }

  @Test
  void createTripWithTheEndBeforeTheStartReturns400() {
    json(anaToken)
        .body(
            "{\"name\":\"Trip\",\"destination\":\"Lisbon\",\"startDate\":\"2027-05-05\","
                + "\"endDate\":\"2027-05-01\"}")
        .post(TRIPS)
        .then()
        .statusCode(400)
        .body("message", equalTo("endDateNotBeforeStartDate: must not be before startDate"));
    assertThat(tripRepository.count()).isZero();
  }

  @Test
  void createTripWithMalformedJsonOrAnImpossibleDateReturns400() {
    json(anaToken).body("{not json").post(TRIPS).then().statusCode(400);
    json(anaToken)
        .body(
            "{\"name\":\"Trip\",\"destination\":\"Lisbon\",\"startDate\":\"2027-13-45\","
                + "\"endDate\":\"2027-05-01\"}")
        .post(TRIPS)
        .then()
        .statusCode(400);
  }

  // --- read ---

  @Test
  void getTripReturnsTheTripToItsCreator() {
    long id = createTrip(anaToken);

    as(anaToken)
        .get(TRIPS + "/" + id)
        .then()
        .statusCode(200)
        .body("id", equalTo((int) id))
        .body("destination", equalTo("Lisbon"));
  }

  @Test
  void getTripOfAnotherUserIs403AndAMissingOneIs404() {
    long id = createTrip(anaToken);

    as(bobToken).get(TRIPS + "/" + id).then().statusCode(403);
    as(anaToken)
        .get(TRIPS + "/999999")
        .then()
        .statusCode(404)
        .body("message", equalTo("Trip not found"));
    as(anaToken).get(TRIPS + "/abc").then().statusCode(400);
  }

  @Test
  void theAdministratorCannotSeeAnotherUsersTripYet() {
    long id = createTrip(anaToken);

    as(adminToken).get(TRIPS + "/" + id).then().statusCode(403);
    as(adminToken).get(TRIPS + "/" + id + "/cover").then().statusCode(403);
  }

  @Test
  void theAdministratorManagesTheirOwnTrips() {
    long id = createTrip(adminToken);

    as(adminToken).get(TRIPS + "/" + id).then().statusCode(200);
    json(adminToken).body(VALID_TRIP).put(TRIPS + "/" + id).then().statusCode(200);
    as(adminToken).delete(TRIPS + "/" + id).then().statusCode(204);
  }

  // --- update ---

  @Test
  void putReplacesEveryFieldAndKeepsTheCover() {
    long id = createTrip(anaToken);
    uploadCover(anaToken, id, ImageTestData.png(100), "image/png").then().statusCode(201);

    json(anaToken)
        .body(
            "{\"name\":\"Oslo\",\"destination\":\"Oslo\",\"startDate\":\"2028-02-01\","
                + "\"endDate\":\"2028-02-03\"}")
        .put(TRIPS + "/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo("Oslo"))
        .body("startDate", equalTo("2028-02-01"))
        .body("budget", nullValue())
        .body("description", nullValue());
    assertThat(coverImageId(anaToken, id)).isNotNull();
  }

  @Test
  void putWithInvalidDataOrEndBeforeStartReturns400AndChangesNothing() {
    long id = createTrip(anaToken);

    json(anaToken)
        .body(
            "{\"name\":\"\",\"destination\":\"Oslo\",\"startDate\":\"2028-02-01\","
                + "\"endDate\":\"2028-02-03\"}")
        .put(TRIPS + "/" + id)
        .then()
        .statusCode(400);
    json(anaToken)
        .body(
            "{\"name\":\"Oslo\",\"destination\":\"Oslo\",\"startDate\":\"2028-02-05\","
                + "\"endDate\":\"2028-02-03\"}")
        .put(TRIPS + "/" + id)
        .then()
        .statusCode(400);
    as(anaToken).get(TRIPS + "/" + id).then().body("name", equalTo("Lisbon trip"));
  }

  @Test
  void putOnAnotherUsersTripIs403AndChangesNothing() {
    long id = createTrip(anaToken);

    json(bobToken)
        .body(VALID_TRIP.replace("Lisbon trip", "Hijacked"))
        .put(TRIPS + "/" + id)
        .then()
        .statusCode(403);
    as(anaToken).get(TRIPS + "/" + id).then().body("name", equalTo("Lisbon trip"));
  }

  @Test
  void putOnAMissingTripIs404() {
    json(anaToken).body(VALID_TRIP).put(TRIPS + "/999999").then().statusCode(404);
  }

  // --- delete ---

  @Test
  void deleteRemovesTheTripAndItsCoverImage() {
    long id = createTrip(anaToken);
    uploadCover(anaToken, id, ImageTestData.png(100), "image/png").then().statusCode(201);
    String imageId = coverImageId(anaToken, id);
    as(anaToken).get("/api/v1/images/" + imageId).then().statusCode(200);

    as(anaToken).delete(TRIPS + "/" + id).then().statusCode(204);

    as(anaToken).get(TRIPS + "/" + id).then().statusCode(404);
    as(anaToken).get("/api/v1/images/" + imageId).then().statusCode(404);
    assertThat(imageRepository.count()).isZero();
  }

  @Test
  void deleteOfAnotherUsersTripIs403AndOfAMissingOneIs404() {
    long id = createTrip(anaToken);

    as(bobToken).delete(TRIPS + "/" + id).then().statusCode(403);
    as(anaToken).get(TRIPS + "/" + id).then().statusCode(200);
    as(anaToken).delete(TRIPS + "/999999").then().statusCode(404);
    as(anaToken).delete(TRIPS + "/abc").then().statusCode(400);
  }

  // --- cover ---

  @Test
  void theFirstCoverAnswers201WithLocationAndAReplacementAnswers200() {
    long id = createTrip(anaToken);

    Response first = uploadCover(anaToken, id, ImageTestData.png(100), "image/png");
    first.then().statusCode(201).header("Location", endsWith(TRIPS + "/" + id + "/cover"));
    String firstImage = first.jsonPath().getString("id");
    Response second = uploadCover(anaToken, id, ImageTestData.jpeg(100), "image/jpeg");
    second.then().statusCode(200).body("contentType", equalTo("image/jpeg"));

    assertThat(coverImageId(anaToken, id)).isEqualTo(second.jsonPath().getString("id"));
    as(anaToken).get("/api/v1/images/" + firstImage).then().statusCode(404);
    assertThat(imageRepository.count()).isEqualTo(1);
  }

  @Test
  void getCoverReturnsTheBytesWithTheirTypeAndNoCache() {
    long id = createTrip(anaToken);
    byte[] data = ImageTestData.png(100);
    uploadCover(anaToken, id, data, "image/png").then().statusCode(201);

    byte[] received =
        as(anaToken)
            .get(TRIPS + "/" + id + "/cover")
            .then()
            .statusCode(200)
            .contentType("image/png")
            .header("Cache-Control", containsString("no-cache"))
            .extract()
            .asByteArray();

    assertThat(received).isEqualTo(data);
  }

  @Test
  void getCoverOfATripWithoutOneOrOfAMissingTripIs404() {
    long id = createTrip(anaToken);

    as(anaToken).get(TRIPS + "/" + id + "/cover").then().statusCode(404);
    as(anaToken).get(TRIPS + "/999999/cover").then().statusCode(404);
  }

  @Test
  void aCoverThatIsNotAnImageIsRejectedAndTheTripKeepsItsCover() {
    long id = createTrip(anaToken);
    uploadCover(anaToken, id, ImageTestData.png(100), "image/png").then().statusCode(201);
    String before = coverImageId(anaToken, id);

    uploadCover(anaToken, id, ImageTestData.text(), "image/png").then().statusCode(400);
    uploadCover(anaToken, id, ImageTestData.png(100), "text/plain").then().statusCode(400);
    uploadCover(anaToken, id, new byte[0], "image/png").then().statusCode(400);

    assertThat(coverImageId(anaToken, id)).isEqualTo(before);
    assertThat(imageRepository.count()).isEqualTo(1);
  }

  @Test
  void aCoverLargerThanTheLimitIs413() {
    long id = createTrip(anaToken);

    uploadCover(anaToken, id, ImageTestData.png(6 * 1024 * 1024), "image/png")
        .then()
        .statusCode(413);
    assertThat(imageRepository.count()).isZero();
  }

  @Test
  void aCoverUploadWithoutTheFilePartIs400() {
    long id = createTrip(anaToken);

    as(anaToken)
        .multiPart("other", "value")
        .put(TRIPS + "/" + id + "/cover")
        .then()
        .statusCode(400)
        .body("message", equalTo("Missing part 'file'"));
  }

  @Test
  void deleteCoverAnswers204AlsoWithoutCoverAndThenTheCoverIsGone() {
    long id = createTrip(anaToken);
    as(anaToken).delete(TRIPS + "/" + id + "/cover").then().statusCode(204);
    uploadCover(anaToken, id, ImageTestData.png(100), "image/png").then().statusCode(201);

    as(anaToken).delete(TRIPS + "/" + id + "/cover").then().statusCode(204);

    as(anaToken).get(TRIPS + "/" + id + "/cover").then().statusCode(404);
    as(anaToken).get(TRIPS + "/" + id).then().body("coverImageId", nullValue());
    assertThat(imageRepository.count()).isZero();
  }

  @Test
  void theCoverOperationsOfAnotherUsersTripAre403AndChangeNothing() {
    long id = createTrip(anaToken);
    uploadCover(anaToken, id, ImageTestData.png(100), "image/png").then().statusCode(201);
    String before = coverImageId(anaToken, id);

    uploadCover(bobToken, id, ImageTestData.png(100), "image/png").then().statusCode(403);
    as(bobToken).get(TRIPS + "/" + id + "/cover").then().statusCode(403);
    as(bobToken).delete(TRIPS + "/" + id + "/cover").then().statusCode(403);

    assertThat(coverImageId(anaToken, id)).isEqualTo(before);
    assertThat(imageRepository.count()).isEqualTo(1);
  }

  @Test
  void theCoverOperationsOfAMissingTripAre404() {
    uploadCover(anaToken, 999999, ImageTestData.png(100), "image/png").then().statusCode(404);
    as(anaToken).delete(TRIPS + "/999999/cover").then().statusCode(404);
  }

  // --- activities (B5) ---

  private long createActivity(String token, long tripId, String name, String date) {
    return json(token)
        .body("{\"name\":\"%s\",\"date\":\"%s\"}".formatted(name, date))
        .post(TRIPS + "/" + tripId + "/activities")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  @Test
  void deleteRemovesTheActivitiesOfTheTripAndTheirImagesAndKeepsThoseOfOtherTrips() {
    long id = createTrip(anaToken);
    long kept = createTrip(anaToken);
    long withImage = createActivity(anaToken, id, "Museum", "2027-05-02");
    createActivity(anaToken, id, "Walk", "2027-05-03");
    long keptActivity = createActivity(anaToken, kept, "Beach", "2027-05-02");
    as(anaToken)
        .multiPart("file", "picture", ImageTestData.png(100), "image/png")
        .put(TRIPS + "/" + id + "/activities/" + withImage + "/image")
        .then()
        .statusCode(201);
    uploadCover(anaToken, id, ImageTestData.png(100), "image/png").then().statusCode(201);
    String activityImage =
        as(anaToken)
            .get(TRIPS + "/" + id + "/activities/" + withImage)
            .then()
            .extract()
            .path("imageId");
    assertThat(imageRepository.count()).isEqualTo(2);

    as(anaToken).delete(TRIPS + "/" + id).then().statusCode(204);

    as(anaToken).get(TRIPS + "/" + id + "/activities").then().statusCode(404);
    as(anaToken).get("/api/v1/images/" + activityImage).then().statusCode(404);
    assertThat(imageRepository.count()).isZero();
    as(anaToken)
        .get(TRIPS + "/" + kept + "/activities/" + keptActivity)
        .then()
        .statusCode(200)
        .body("name", equalTo("Beach"));
  }

  @Test
  void putWithDatesThatLeaveAnActivityOutsideReturns409AndChangesNothingUntilItIsMoved() {
    long id = createTrip(anaToken);
    long activity = createActivity(anaToken, id, "Last day", "2027-05-05");
    String shorter =
        "{\"name\":\"Lisbon trip\",\"destination\":\"Lisbon\",\"startDate\":\"2027-05-01\","
            + "\"endDate\":\"2027-05-03\"}";

    json(anaToken)
        .body(shorter)
        .put(TRIPS + "/" + id)
        .then()
        .statusCode(409)
        .body(
            "message",
            equalTo("Some activities fall outside the new dates; move or delete them first"));
    as(anaToken).get(TRIPS + "/" + id).then().body("endDate", equalTo("2027-05-05"));

    json(anaToken)
        .body("{\"name\":\"Last day\",\"date\":\"2027-05-02\"}")
        .put(TRIPS + "/" + id + "/activities/" + activity)
        .then()
        .statusCode(200);
    json(anaToken)
        .body(shorter)
        .put(TRIPS + "/" + id)
        .then()
        .statusCode(200)
        .body("endDate", equalTo("2027-05-03"));
  }

  @Test
  void putThatKeepsTheDatesOrWidensThemIsNotBlockedByActivities() {
    long id = createTrip(anaToken);
    createActivity(anaToken, id, "Last day", "2027-05-05");

    json(anaToken)
        .body(VALID_TRIP.replace("Lisbon trip", "Renamed"))
        .put(TRIPS + "/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo("Renamed"));
    json(anaToken)
        .body(VALID_TRIP.replace("2027-05-05", "2027-05-09"))
        .put(TRIPS + "/" + id)
        .then()
        .statusCode(200)
        .body("endDate", equalTo("2027-05-09"));
  }
}
