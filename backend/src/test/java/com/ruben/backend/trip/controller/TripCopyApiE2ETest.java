package com.ruben.backend.trip.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AdminAccountInitializer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
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

/** I9: copying a public itinerary through the real HTTP API. Ana owns it, Bob copies it. */
@Tag("I9")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class TripCopyApiE2ETest {

  private static final String PASSWORD = "secret-pass";
  private static final String TRIPS = "/api/v1/trips";
  private static final String VALID_TRIP =
      "{\"name\":\"Rome trip\",\"destination\":\"Rome\",\"startDate\":\"2027-07-10\","
          + "\"endDate\":\"2027-07-17\",\"budget\":900.00,\"description\":\"Ancient ruins\"}";

  @LocalServerPort private int port;

  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AdminAccountInitializer adminAccountInitializer;

  private String anaToken;
  private String bobToken;
  private long originalId;
  private String copies;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "https://localhost";
    RestAssured.port = port;
    RestAssured.useRelaxedHTTPSValidation();
    databaseCleaner.clear();
    adminAccountInitializer.run(new DefaultApplicationArguments());
    newUser("Ana", "ana@example.com");
    newUser("Bob", "bob@example.com");
    anaToken = tokenOf("ana@example.com");
    bobToken = tokenOf("bob@example.com");
    originalId = publishedTripWithCoverAndActivities();
    copies = TRIPS + "/public/" + originalId + "/copies";
  }

  @AfterEach
  void tearDown() {
    RestAssured.reset();
  }

  private void newUser(String name, String email) {
    userRepository.save(new User(name, email, passwordEncoder.encode(PASSWORD), Role.USER));
  }

  private static String tokenOf(String email) {
    return given()
        .contentType(ContentType.JSON)
        .body("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD))
        .post("/api/v1/auth/login")
        .then()
        .statusCode(200)
        .extract()
        .cookie("AuthToken");
  }

  private static RequestSpecification json(String token) {
    return given().cookie("AuthToken", token).contentType(ContentType.JSON);
  }

  private long createActivity(long tripId, String name, String date, boolean withPicture) {
    long id =
        json(anaToken)
            .body(
                "{\"name\":\"%s\",\"date\":\"%s\",\"startTime\":\"10:00\",\"endTime\":\"12:00\"}"
                    .formatted(name, date))
            .post(TRIPS + "/" + tripId + "/activities")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
    if (withPicture) {
      given()
          .cookie("AuthToken", anaToken)
          .multiPart("file", "picture", ImageTestData.jpeg(40), "image/jpeg")
          .put(TRIPS + "/" + tripId + "/activities/" + id + "/image")
          .then()
          .statusCode(201);
    }
    return id;
  }

  /** Ana's published trip: a cover, an activity with a picture and one without. */
  private long publishedTripWithCoverAndActivities() {
    long id =
        json(anaToken)
            .body(VALID_TRIP)
            .post(TRIPS)
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
    given()
        .cookie("AuthToken", anaToken)
        .multiPart("file", "cover", ImageTestData.png(30), "image/png")
        .put(TRIPS + "/" + id + "/cover")
        .then()
        .statusCode(201);
    createActivity(id, "Colosseum", "2027-07-11", true);
    createActivity(id, "Forum", "2027-07-13", false);
    json(anaToken)
        .body("{\"published\":true}")
        .put(TRIPS + "/" + id + "/publication")
        .then()
        .statusCode(200);
    return id;
  }

  private long copyOf(String body) {
    return json(bobToken)
        .body(body)
        .post(copies)
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private List<String> activityDates(String token, long tripId) {
    return json(token)
        .get(TRIPS + "/" + tripId + "/activities?sort=date,asc")
        .then()
        .statusCode(200)
        .extract()
        .jsonPath()
        .getList("content.date");
  }

  // --- who may copy, and what ---

  @Test
  void anAnonymousUserCannotCopyAPublicTrip() {
    given().contentType(ContentType.JSON).post(copies).then().statusCode(401);
    json(bobToken).get(TRIPS).then().body("content", hasSize(0));
  }

  @Test
  void copyingAnUnknownTripIsNotFound() {
    json(bobToken).post(TRIPS + "/public/999999/copies").then().statusCode(404);
  }

  @Test
  void copyingAnUnpublishedTripIsNotFoundNeverForbidden() {
    json(anaToken)
        .body("{\"published\":false}")
        .put(TRIPS + "/" + originalId + "/publication")
        .then()
        .statusCode(200);

    json(bobToken).post(copies).then().statusCode(404);
    json(anaToken).post(copies).then().statusCode(404);
    json(bobToken).get(TRIPS).then().body("content", hasSize(0));
  }

  @Test
  void aTextInsteadOfANumberAsIdIsABadRequest() {
    json(bobToken).post(TRIPS + "/public/abc/copies").then().statusCode(400);
  }

  @Test
  void anInvalidStartDateIsABadRequestAndCopiesNothing() {
    json(bobToken).body("{\"startDate\":\"next summer\"}").post(copies).then().statusCode(400);
    json(bobToken).get(TRIPS).then().body("content", hasSize(0));
  }

  // --- the copy ---

  @Test
  void theCopyIsCreatedForTheCallerUnpublishedAndLinkedToTheOriginal() {
    json(bobToken)
        .post(copies)
        .then()
        .statusCode(201)
        .header("Location", endsWith("/api/v1/trips/" + (originalId + 1)))
        .body("id", notNullValue())
        .body("name", equalTo("Rome trip"))
        .body("destination", equalTo("Rome"))
        .body("budget", equalTo(900.00f))
        .body("description", equalTo("Ancient ruins"))
        .body("published", equalTo(false))
        .body("publishedAt", nullValue())
        .body("copiedFromId", equalTo((int) originalId));

    json(bobToken).get(TRIPS).then().statusCode(200).body("content", hasSize(1));
    json(anaToken).get(TRIPS).then().body("content", hasSize(1));
  }

  @Test
  void theCopyBelongsToTheCallerAloneAndAnaCannotSeeIt() {
    long copyId = copyOf("{}");

    json(bobToken)
        .get(TRIPS + "/" + copyId)
        .then()
        .statusCode(200)
        .body("copiedFromId", equalTo((int) originalId));
    json(anaToken).get(TRIPS + "/" + copyId).then().statusCode(403);
    given().get(TRIPS + "/public/" + copyId).then().statusCode(404);
  }

  @Test
  void withoutABodyTheOriginalDatesAreKept() {
    long copyId =
        json(bobToken).post(copies).then().statusCode(201).extract().jsonPath().getLong("id");

    json(bobToken)
        .get(TRIPS + "/" + copyId)
        .then()
        .body("startDate", equalTo("2027-07-10"))
        .body("endDate", equalTo("2027-07-17"));
    assertThat(activityDates(bobToken, copyId)).containsExactly("2027-07-11", "2027-07-13");
  }

  @Test
  void withAnEmptyBodyOrANullDateTheOriginalDatesAreKept() {
    long emptyBody = copyOf("{}");
    long nullDate = copyOf("{\"startDate\":null}");

    assertThat(activityDates(bobToken, emptyBody)).containsExactly("2027-07-11", "2027-07-13");
    assertThat(activityDates(bobToken, nullDate)).containsExactly("2027-07-11", "2027-07-13");
  }

  @Test
  void withAStartDateTheTripKeepsItsLengthAndEveryActivityMovesByTheSameDays() {
    long copyId = copyOf("{\"startDate\":\"2027-09-01\"}");

    json(bobToken)
        .get(TRIPS + "/" + copyId)
        .then()
        .body("startDate", equalTo("2027-09-01"))
        .body("endDate", equalTo("2027-09-08"));
    assertThat(activityDates(bobToken, copyId)).containsExactly("2027-09-02", "2027-09-04");
    assertThat(activityDates(anaToken, originalId)).containsExactly("2027-07-11", "2027-07-13");
  }

  @Test
  void theCopiedActivitiesAreTheCallersAndKeepTheirTimes() {
    long copyId = copyOf("{}");
    long bobId = json(bobToken).get("/api/v1/auth/me").then().extract().jsonPath().getLong("id");

    json(bobToken)
        .get(TRIPS + "/" + copyId + "/activities?sort=date,asc")
        .then()
        .statusCode(200)
        .body("content", hasSize(2))
        .body("content[0].name", equalTo("Colosseum"))
        .body("content[0].startTime", equalTo("10:00:00"))
        .body("content[0].endTime", equalTo("12:00:00"))
        .body("content[0].authorId", equalTo((int) bobId))
        .body("content[1].name", equalTo("Forum"));
  }

  @Test
  void theCoverAndThePicturesAreNewImagesWithTheSameContent() {
    String originalCover =
        json(anaToken).get(TRIPS + "/" + originalId).then().extract().path("coverImageId");
    String originalPicture =
        json(anaToken)
            .get(TRIPS + "/" + originalId + "/activities?sort=date,asc")
            .then()
            .extract()
            .path("content[0].imageId");
    long copyId = copyOf("{}");

    String copiedCover =
        json(bobToken).get(TRIPS + "/" + copyId).then().extract().path("coverImageId");
    String copiedPicture =
        json(bobToken)
            .get(TRIPS + "/" + copyId + "/activities?sort=date,asc")
            .then()
            .extract()
            .path("content[0].imageId");
    String copiedWithoutPicture =
        json(bobToken)
            .get(TRIPS + "/" + copyId + "/activities?sort=date,asc")
            .then()
            .extract()
            .path("content[1].imageId");

    assertThat(copiedCover).isNotNull().isNotEqualTo(originalCover);
    assertThat(copiedPicture).isNotNull().isNotEqualTo(originalPicture);
    assertThat(copiedWithoutPicture).isNull();
    byte[] cover =
        given()
            .get("/api/v1/images/" + copiedCover)
            .then()
            .statusCode(200)
            .contentType("image/png")
            .extract()
            .asByteArray();
    assertThat(cover).isEqualTo(ImageTestData.png(30));
    byte[] picture =
        given()
            .get("/api/v1/images/" + copiedPicture)
            .then()
            .statusCode(200)
            .contentType("image/jpeg")
            .extract()
            .asByteArray();
    assertThat(picture).isEqualTo(ImageTestData.jpeg(40));
  }

  @Test
  void aTripCanBeCopiedMoreThanOnceAndByItsOwnCreator() {
    long first = copyOf("{}");
    long second = copyOf("{}");
    long ownCopy =
        json(anaToken).post(copies).then().statusCode(201).extract().jsonPath().getLong("id");

    assertThat(List.of(first, second, ownCopy)).doesNotHaveDuplicates().doesNotContain(originalId);
    json(bobToken).get(TRIPS).then().body("content", hasSize(2));
    json(anaToken).get(TRIPS).then().body("content", hasSize(2));
  }

  // --- independence from the original ---

  @Test
  void editingOrUnpublishingTheOriginalDoesNotChangeTheCopy() {
    long copyId = copyOf("{}");

    json(anaToken)
        .body(
            "{\"name\":\"Renamed\",\"destination\":\"Paris\",\"startDate\":\"2028-01-01\","
                + "\"endDate\":\"2028-01-03\"}")
        .put(TRIPS + "/" + originalId)
        .then()
        .statusCode(200);
    json(anaToken)
        .body("{\"published\":false}")
        .put(TRIPS + "/" + originalId + "/publication")
        .then()
        .statusCode(200);

    json(bobToken)
        .get(TRIPS + "/" + copyId)
        .then()
        .body("name", equalTo("Rome trip"))
        .body("destination", equalTo("Rome"))
        .body("startDate", equalTo("2027-07-10"))
        .body("copiedFromId", equalTo((int) originalId));
  }

  @Test
  void deletingTheOriginalKeepsTheCopyWithItsImagesAndClearsTheLink() {
    long copyId = copyOf("{}");
    String copiedCover =
        json(bobToken).get(TRIPS + "/" + copyId).then().extract().path("coverImageId");
    String copiedPicture =
        json(bobToken)
            .get(TRIPS + "/" + copyId + "/activities?sort=date,asc")
            .then()
            .extract()
            .path("content[0].imageId");

    json(anaToken).delete(TRIPS + "/" + originalId).then().statusCode(204);

    json(bobToken)
        .get(TRIPS + "/" + copyId)
        .then()
        .statusCode(200)
        .body("copiedFromId", nullValue())
        .body("name", equalTo("Rome trip"))
        .body("coverImageId", equalTo(copiedCover));
    assertThat(activityDates(bobToken, copyId)).hasSize(2);
    given().get("/api/v1/images/" + copiedCover).then().statusCode(200);
    given().get("/api/v1/images/" + copiedPicture).then().statusCode(200);
  }

  @Test
  void deletingTheCopyLeavesTheOriginalIntact() {
    long copyId = copyOf("{}");

    json(bobToken).delete(TRIPS + "/" + copyId).then().statusCode(204);

    given().get(TRIPS + "/public/" + originalId).then().statusCode(200);
    assertThat(activityDates(anaToken, originalId)).hasSize(2);
    String cover =
        json(anaToken).get(TRIPS + "/" + originalId).then().extract().path("coverImageId");
    given().get("/api/v1/images/" + cover).then().statusCode(200);
  }
}
