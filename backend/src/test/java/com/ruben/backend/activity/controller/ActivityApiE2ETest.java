package com.ruben.backend.activity.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.activity.repository.ActivityRepository;
import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.image.repository.ImageRepository;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AdminAccountInitializer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
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

@Tag("B5")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ActivityApiE2ETest {

  private static final String PASSWORD = "secret-pass";
  private static final String TRIPS = "/api/v1/trips";
  private static final String TRIP_BODY =
      "{\"name\":\"Rome\",\"destination\":\"Rome\",\"startDate\":\"2027-07-10\","
          + "\"endDate\":\"2027-07-14\"}";

  @LocalServerPort private int port;

  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private UserRepository userRepository;
  @Autowired private ActivityRepository activityRepository;
  @Autowired private ImageRepository imageRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AdminAccountInitializer adminAccountInitializer;

  private String anaToken;
  private String bobToken;
  private String adminToken;
  private long tripId;
  private String activities;

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
    tripId = createTrip(anaToken);
    activities = TRIPS + "/" + tripId + "/activities";
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

  private static long createTrip(String token) {
    return json(token)
        .body(TRIP_BODY)
        .post(TRIPS)
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private static String body(String name, String date) {
    return "{\"name\":\"%s\",\"date\":\"%s\"}".formatted(name, date);
  }

  private static String bodyWithTimes(String name, String date, String start, String end) {
    return ("{\"name\":\"%s\",\"date\":\"%s\",\"startTime\":%s,\"endTime\":%s}")
        .formatted(
            name,
            date,
            start == null ? "null" : "\"" + start + "\"",
            end == null ? "null" : "\"" + end + "\"");
  }

  private long createActivity(String name, String date) {
    return createActivityWith(body(name, date));
  }

  private long createActivityWith(String requestBody) {
    return json(anaToken)
        .body(requestBody)
        .post(activities)
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private Response uploadImage(String token, String url, byte[] data, String type) {
    return as(token).multiPart("file", "picture", data, type).put(url + "/image");
  }

  private String imageId(long activityId) {
    return as(anaToken)
        .get(activities + "/" + activityId)
        .then()
        .statusCode(200)
        .extract()
        .path("imageId");
  }

  // --- authentication ---

  @Test
  void everyActivityOperationRequiresLogin() {
    String item = activities + "/1";

    given().get(activities).then().statusCode(401);
    given()
        .contentType(ContentType.JSON)
        .body(body("A", "2027-07-10"))
        .post(activities)
        .then()
        .statusCode(401);
    given().get(item).then().statusCode(401);
    given()
        .contentType(ContentType.JSON)
        .body(body("A", "2027-07-10"))
        .put(item)
        .then()
        .statusCode(401);
    given().delete(item).then().statusCode(401);
    given()
        .multiPart("file", "picture", ImageTestData.png(10), "image/png")
        .put(item + "/image")
        .then()
        .statusCode(401);
    given().get(item + "/image").then().statusCode(401);
    given().delete(item + "/image").then().statusCode(401);
  }

  // --- list and filter ---

  @Test
  void listIsSortedByDateAndStartTimeAndAnActivityWithoutTimeComesFirstOnItsDay() {
    createActivityWith(bodyWithTimes("Dinner", "2027-07-11", "20:00", "22:00"));
    createActivityWith(bodyWithTimes("Museum", "2027-07-11", "10:00", null));
    createActivity("Market", "2027-07-11");
    createActivity("Arrival", "2027-07-10");

    as(anaToken)
        .get(activities)
        .then()
        .statusCode(200)
        .body("content.name", contains("Arrival", "Market", "Museum", "Dinner"))
        .body("totalElements", equalTo(4))
        .body("page", equalTo(0))
        .body("last", equalTo(true));
  }

  @Test
  void listFiltersByDay() {
    createActivity("Day one", "2027-07-10");
    createActivity("Day two A", "2027-07-11");
    createActivity("Day two B", "2027-07-11");

    as(anaToken)
        .queryParam("date", "2027-07-11")
        .get(activities)
        .then()
        .statusCode(200)
        .body("content", hasSize(2))
        .body("content.date", contains("2027-07-11", "2027-07-11"))
        .body("totalElements", equalTo(2));
    as(anaToken)
        .queryParam("date", "2027-07-13")
        .get(activities)
        .then()
        .statusCode(200)
        .body("content", hasSize(0))
        .body("last", equalTo(true));
  }

  @Test
  void listIsPaginated() {
    for (int i = 1; i <= 12; i++) {
      createActivityWith(
          bodyWithTimes("Activity " + i, "2027-07-10", "%02d:00".formatted(i), null));
    }

    as(anaToken)
        .get(activities)
        .then()
        .statusCode(200)
        .body("content", hasSize(10))
        .body("last", equalTo(false))
        .body("totalElements", equalTo(12));
    as(anaToken)
        .queryParam("page", 1)
        .get(activities)
        .then()
        .statusCode(200)
        .body("content.name", contains("Activity 11", "Activity 12"))
        .body("last", equalTo(true));
    as(anaToken)
        .queryParam("page", 5)
        .get(activities)
        .then()
        .statusCode(200)
        .body("content", hasSize(0));
  }

  @Test
  void listWithABadDateOrSortReturns400() {
    as(anaToken).queryParam("date", "11/07/2027").get(activities).then().statusCode(400);
    as(anaToken).queryParam("date", "not-a-date").get(activities).then().statusCode(400);
    as(anaToken).queryParam("sort", "trip.creator.password").get(activities).then().statusCode(400);
  }

  @Test
  void listHonoursTheSizeParameterUpToTheMaximum() {
    as(anaToken)
        .queryParam("size", 5)
        .get(activities)
        .then()
        .statusCode(200)
        .body("size", equalTo(5));
    as(anaToken)
        .queryParam("size", 1000)
        .get(activities)
        .then()
        .statusCode(200)
        .body("size", equalTo(50));
  }

  @Test
  void listCanBeSortedByNameDescending() {
    createActivity("A", "2027-07-10");
    createActivity("B", "2027-07-10");

    as(anaToken)
        .queryParam("sort", "name,desc")
        .get(activities)
        .then()
        .statusCode(200)
        .body("content.name", contains("B", "A"));
  }

  // --- create ---

  @Test
  void createReturnsEveryFieldAndTheLocationOfTheNewActivity() {
    Response response =
        json(anaToken)
            .body(
                "{\"name\":\"  Colosseum \",\"date\":\"2027-07-11\",\"startTime\":\"09:30\","
                    + "\"endTime\":\"11:00\",\"location\":\"Piazza\",\"description\":\"Tour\","
                    + "\"cost\":16.5,\"category\":\"SIGHTSEEING\"}")
            .post(activities);

    long id = response.jsonPath().getLong("id");
    response
        .then()
        .statusCode(201)
        .header("Location", endsWith(activities + "/" + id))
        .body("tripId", equalTo((int) tripId))
        .body("authorId", notNullValue())
        .body("name", equalTo("Colosseum"))
        .body("date", equalTo("2027-07-11"))
        .body("startTime", equalTo("09:30:00"))
        .body("endTime", equalTo("11:00:00"))
        .body("location", equalTo("Piazza"))
        .body("description", equalTo("Tour"))
        .body("cost", equalTo(16.5f))
        .body("category", equalTo("SIGHTSEEING"))
        .body("imageId", nullValue());
  }

  @Test
  void createWithOnlyTheNameAndTheDateIsValid() {
    json(anaToken)
        .body(body("Walk", "2027-07-10"))
        .post(activities)
        .then()
        .statusCode(201)
        .body("startTime", nullValue())
        .body("cost", nullValue())
        .body("category", nullValue());
  }

  @Test
  void createWithAnInvalidBodyReturns400AndStoresNothing() {
    String[] invalid = {
      "{\"name\":\"\",\"date\":\"2027-07-10\"}",
      "{\"name\":\"   \",\"date\":\"2027-07-10\"}",
      "{\"date\":\"2027-07-10\"}",
      "{\"name\":\"A\"}",
      "{\"name\":\"" + "a".repeat(101) + "\",\"date\":\"2027-07-10\"}",
      "{\"name\":\"A\",\"date\":\"2027-07-10\",\"location\":\"" + "l".repeat(201) + "\"}",
      "{\"name\":\"A\",\"date\":\"2027-07-10\",\"description\":\"" + "d".repeat(2001) + "\"}",
      "{\"name\":\"A\",\"date\":\"2027-07-10\",\"cost\":-1}",
      "{\"name\":\"A\",\"date\":\"2027-07-10\",\"cost\":1.234}",
      "{\"name\":\"A\",\"date\":\"2027-07-10\",\"category\":\"PARTY\"}",
      "{\"name\":\"A\",\"date\":\"2027-02-30\"}",
      "{\"name\":\"A\",\"date\":\"2027-07-10\",\"startTime\":\"25:00\"}",
      "{\"name\":",
    };

    for (String requestBody : invalid) {
      json(anaToken).body(requestBody).post(activities).then().statusCode(400);
    }

    assertThat(activityRepository.count()).isZero();
  }

  @Test
  void createWithTheEndNotAfterTheStartOrWithoutStartReturns400() {
    json(anaToken)
        .body(bodyWithTimes("A", "2027-07-10", "12:00", "10:00"))
        .post(activities)
        .then()
        .statusCode(400)
        .body("message", containsString("endTimeAfterStartTime: must be after startTime"));
    json(anaToken)
        .body(bodyWithTimes("A", "2027-07-10", "10:00", "10:00"))
        .post(activities)
        .then()
        .statusCode(400);
    json(anaToken)
        .body(bodyWithTimes("A", "2027-07-10", null, "10:00"))
        .post(activities)
        .then()
        .statusCode(400)
        .body("message", containsString("endTimeWithStartTime: requires startTime"));

    assertThat(activityRepository.count()).isZero();
  }

  @Test
  void createWithADateOutsideTheTripReturns400ButTheFirstAndLastDayAreValid() {
    json(anaToken)
        .body(body("Early", "2027-07-09"))
        .post(activities)
        .then()
        .statusCode(400)
        .body("message", equalTo("The date is outside the trip's dates"));
    json(anaToken).body(body("Late", "2027-07-15")).post(activities).then().statusCode(400);
    json(anaToken).body(body("First", "2027-07-10")).post(activities).then().statusCode(201);
    json(anaToken).body(body("Last", "2027-07-14")).post(activities).then().statusCode(201);

    assertThat(activityRepository.count()).isEqualTo(2);
  }

  // --- read ---

  @Test
  void getReturnsTheActivityToTheTripsCreator() {
    long id = createActivity("Museum", "2027-07-11");

    as(anaToken)
        .get(activities + "/" + id)
        .then()
        .statusCode(200)
        .body("id", equalTo((int) id))
        .body("name", equalTo("Museum"));
  }

  @Test
  void anActivityOfAnotherTripIsNotFoundThroughThisOne() {
    long id = createActivity("Museum", "2027-07-11");
    long otherTrip = createTrip(anaToken);
    String foreign = TRIPS + "/" + otherTrip + "/activities/" + id;

    as(anaToken).get(foreign).then().statusCode(404);
    json(anaToken).body(body("Moved", "2027-07-11")).put(foreign).then().statusCode(404);
    as(anaToken).delete(foreign).then().statusCode(404);
    uploadImage(anaToken, foreign, ImageTestData.png(10), "image/png").then().statusCode(404);
    as(anaToken).get(foreign + "/image").then().statusCode(404);
    as(anaToken).delete(foreign + "/image").then().statusCode(404);

    as(anaToken).get(activities + "/" + id).then().statusCode(200).body("name", equalTo("Museum"));
    assertThat(imageRepository.count()).isZero();
  }

  @Test
  void aMissingActivityOrTripIs404AndABadIdIs400() {
    as(anaToken).get(activities + "/999999").then().statusCode(404);
    json(anaToken).body(body("A", "2027-07-10")).put(activities + "/999999").then().statusCode(404);
    as(anaToken).delete(activities + "/999999").then().statusCode(404);
    as(anaToken).get(TRIPS + "/999999/activities").then().statusCode(404);
    json(anaToken)
        .body(body("A", "2027-07-10"))
        .post(TRIPS + "/999999/activities")
        .then()
        .statusCode(404);
    as(anaToken).get(activities + "/abc").then().statusCode(400);
    as(anaToken).get(TRIPS + "/abc/activities").then().statusCode(400);
  }

  // --- permissions ---

  @Test
  void anotherUserCannotDoAnythingWithTheActivitiesOfTheTrip() {
    long id = createActivity("Museum", "2027-07-11");
    String item = activities + "/" + id;
    uploadImage(anaToken, item, ImageTestData.png(100), "image/png").then().statusCode(201);
    String image = imageId(id);

    as(bobToken).get(activities).then().statusCode(403);
    json(bobToken).body(body("Mine", "2027-07-10")).post(activities).then().statusCode(403);
    as(bobToken).get(item).then().statusCode(403);
    json(bobToken).body(body("Mine", "2027-07-10")).put(item).then().statusCode(403);
    as(bobToken).delete(item).then().statusCode(403);
    uploadImage(bobToken, item, ImageTestData.png(100), "image/png").then().statusCode(403);
    as(bobToken).get(item + "/image").then().statusCode(403);
    as(bobToken).delete(item + "/image").then().statusCode(403);

    as(anaToken).get(item).then().statusCode(200).body("name", equalTo("Museum"));
    assertThat(imageId(id)).isEqualTo(image);
    assertThat(activityRepository.count()).isEqualTo(1);
    assertThat(imageRepository.count()).isEqualTo(1);
  }

  @Test
  void theAdministratorCannotTouchAnotherUsersActivitiesYet() {
    long id = createActivity("Museum", "2027-07-11");

    as(adminToken).get(activities).then().statusCode(403);
    as(adminToken).get(activities + "/" + id).then().statusCode(403);
    as(adminToken).delete(activities + "/" + id).then().statusCode(403);
  }

  @Test
  void aValidationErrorComesBeforeThePermissionCheckAndALookupBeforeTheActivityLookup() {
    json(bobToken).body("{\"name\":\"\"}").post(activities).then().statusCode(400);
    as(bobToken).get(activities + "/999999").then().statusCode(403);
  }

  // --- update ---

  @Test
  void putReplacesEveryFieldAndKeepsTheImage() {
    long id =
        createActivityWith(
            "{\"name\":\"Old\",\"date\":\"2027-07-10\",\"startTime\":\"08:00\","
                + "\"location\":\"Here\",\"cost\":3,\"category\":\"FOOD\"}");
    uploadImage(anaToken, activities + "/" + id, ImageTestData.png(100), "image/png")
        .then()
        .statusCode(201);
    String image = imageId(id);

    json(anaToken)
        .body(body("New", "2027-07-12"))
        .put(activities + "/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo("New"))
        .body("date", equalTo("2027-07-12"))
        .body("startTime", nullValue())
        .body("location", nullValue())
        .body("cost", nullValue())
        .body("category", nullValue())
        .body("imageId", equalTo(image));
    as(anaToken).get(activities + "/" + id).then().body("name", equalTo("New"));
  }

  @Test
  void putWithInvalidDataOrADateOutsideTheTripReturns400AndChangesNothing() {
    long id = createActivity("Museum", "2027-07-11");

    json(anaToken)
        .body("{\"name\":\"\",\"date\":\"2027-07-11\"}")
        .put(activities + "/" + id)
        .then()
        .statusCode(400);
    json(anaToken)
        .body(bodyWithTimes("Museum", "2027-07-11", "12:00", "10:00"))
        .put(activities + "/" + id)
        .then()
        .statusCode(400);
    json(anaToken)
        .body(body("Museum", "2027-08-01"))
        .put(activities + "/" + id)
        .then()
        .statusCode(400);

    as(anaToken).get(activities + "/" + id).then().body("date", equalTo("2027-07-11"));
  }

  // --- delete ---

  @Test
  void deleteAnswers204AndThenTheActivityAndItsImageAreGone() {
    long id = createActivity("Museum", "2027-07-11");
    uploadImage(anaToken, activities + "/" + id, ImageTestData.png(100), "image/png")
        .then()
        .statusCode(201);
    String image = imageId(id);
    as(anaToken).get("/api/v1/images/" + image).then().statusCode(200);

    as(anaToken).delete(activities + "/" + id).then().statusCode(204);

    as(anaToken).get(activities + "/" + id).then().statusCode(404);
    as(anaToken).get("/api/v1/images/" + image).then().statusCode(404);
    assertThat(imageRepository.count()).isZero();
  }

  // --- image ---

  @Test
  void theFirstImageAnswers201WithLocationAndAReplacementAnswers200() {
    long id = createActivity("Museum", "2027-07-11");
    String item = activities + "/" + id;

    Response first = uploadImage(anaToken, item, ImageTestData.png(100), "image/png");
    first.then().statusCode(201).header("Location", endsWith(item + "/image"));
    String firstImage = first.jsonPath().getString("id");
    Response second = uploadImage(anaToken, item, ImageTestData.jpeg(100), "image/jpeg");
    second.then().statusCode(200).body("contentType", equalTo("image/jpeg"));

    assertThat(imageId(id)).isEqualTo(second.jsonPath().getString("id"));
    as(anaToken).get("/api/v1/images/" + firstImage).then().statusCode(404);
    assertThat(imageRepository.count()).isEqualTo(1);
  }

  @Test
  void getImageReturnsTheBytesWithTheirTypeAndNoCache() {
    long id = createActivity("Museum", "2027-07-11");
    byte[] data = ImageTestData.png(100);
    uploadImage(anaToken, activities + "/" + id, data, "image/png").then().statusCode(201);

    byte[] received =
        as(anaToken)
            .get(activities + "/" + id + "/image")
            .then()
            .statusCode(200)
            .contentType("image/png")
            .header("Cache-Control", containsString("no-cache"))
            .extract()
            .asByteArray();

    assertThat(received).isEqualTo(data);
  }

  @Test
  void getImageOfAnActivityWithoutOneIs404() {
    long id = createActivity("Museum", "2027-07-11");

    as(anaToken).get(activities + "/" + id + "/image").then().statusCode(404);
  }

  @Test
  void anImageThatIsNotValidIsRejectedAndTheActivityKeepsItsImage() {
    long id = createActivity("Museum", "2027-07-11");
    String item = activities + "/" + id;
    uploadImage(anaToken, item, ImageTestData.png(100), "image/png").then().statusCode(201);
    String before = imageId(id);

    uploadImage(anaToken, item, ImageTestData.text(), "image/png").then().statusCode(400);
    uploadImage(anaToken, item, ImageTestData.png(100), "text/plain").then().statusCode(400);
    uploadImage(anaToken, item, new byte[0], "image/png").then().statusCode(400);

    assertThat(imageId(id)).isEqualTo(before);
    assertThat(imageRepository.count()).isEqualTo(1);
  }

  @Test
  void anImageLargerThanTheLimitIs413() {
    long id = createActivity("Museum", "2027-07-11");

    uploadImage(anaToken, activities + "/" + id, ImageTestData.png(6 * 1024 * 1024), "image/png")
        .then()
        .statusCode(413);
    assertThat(imageRepository.count()).isZero();
  }

  @Test
  void anImageUploadWithoutTheFilePartIs400() {
    long id = createActivity("Museum", "2027-07-11");

    as(anaToken)
        .multiPart("other", "value")
        .put(activities + "/" + id + "/image")
        .then()
        .statusCode(400)
        .body("message", equalTo("Missing part 'file'"));
  }

  @Test
  void deleteImageAnswers204AlsoWithoutImageAndThenTheImageIsGone() {
    long id = createActivity("Museum", "2027-07-11");
    String item = activities + "/" + id;
    as(anaToken).delete(item + "/image").then().statusCode(204);
    uploadImage(anaToken, item, ImageTestData.png(100), "image/png").then().statusCode(201);

    as(anaToken).delete(item + "/image").then().statusCode(204);

    as(anaToken).get(item + "/image").then().statusCode(404);
    as(anaToken).get(item).then().body("imageId", nullValue());
    assertThat(imageRepository.count()).isZero();
  }

  // --- cost and remaining budget ---

  private long createBudgetedTrip() {
    return json(anaToken)
        .body(TRIP_BODY.replace("}", ",\"budget\":100}"))
        .post(TRIPS)
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  @Test
  void costRequiresLoginAndTheTripToBeVisible() {
    given().get(activities + "/cost").then().statusCode(401);
    as(bobToken).get(activities + "/cost").then().statusCode(403);
    as(adminToken).get(activities + "/cost").then().statusCode(403);
    as(anaToken).get(TRIPS + "/999999/activities/cost").then().statusCode(404);
    as(bobToken).get(TRIPS + "/999999/activities/cost").then().statusCode(404);
    as(anaToken).get(TRIPS + "/abc/activities/cost").then().statusCode(400);
  }

  @Test
  void costOfATripWithoutBudgetHasNoRemainingBudget() {
    json(anaToken)
        .body("{\"name\":\"Dinner\",\"date\":\"2027-07-10\",\"cost\":40.5}")
        .post(activities)
        .then()
        .statusCode(201);

    as(anaToken)
        .get(activities + "/cost")
        .then()
        .statusCode(200)
        .body("totalCost", equalTo(40.5f))
        .body("remainingBudget", nullValue());
  }

  @Test
  void costSubtractsTheActivitiesFromTheBudgetAndGoesNegativeWhenOver() {
    long budgeted = createBudgetedTrip();
    String url = TRIPS + "/" + budgeted + "/activities";
    var empty = as(anaToken).get(url + "/cost").then().statusCode(200).extract().jsonPath();
    assertThat(empty.getFloat("totalCost")).isZero();
    assertThat(empty.getFloat("remainingBudget")).isEqualTo(100f);

    json(anaToken)
        .body("{\"name\":\"Tour\",\"date\":\"2027-07-10\",\"cost\":60.25}")
        .post(url)
        .then()
        .statusCode(201);
    as(anaToken)
        .get(url + "/cost")
        .then()
        .body("totalCost", equalTo(60.25f))
        .body("remainingBudget", equalTo(39.75f));

    json(anaToken)
        .body("{\"name\":\"Hotel\",\"date\":\"2027-07-11\",\"cost\":50}")
        .post(url)
        .then()
        .statusCode(201);
    as(anaToken)
        .get(url + "/cost")
        .then()
        .body("totalCost", equalTo(110.25f))
        .body("remainingBudget", equalTo(-10.25f));
  }
}
