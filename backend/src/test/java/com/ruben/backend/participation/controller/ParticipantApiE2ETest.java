package com.ruben.backend.participation.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.participation.repository.ParticipationRepository;
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

@Tag("I3")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ParticipantApiE2ETest {

  private static final String PASSWORD = "secret-pass";
  private static final String TRIPS = "/api/v1/trips";
  private static final String RECEIVED = "/api/v1/invitations";
  private static final String TRIP_BODY =
      "{\"name\":\"Rome\",\"destination\":\"Rome\",\"startDate\":\"2027-07-10\","
          + "\"endDate\":\"2027-07-14\"}";
  private static final String ACCEPTED = "{\"status\":\"ACCEPTED\"}";

  @LocalServerPort private int port;

  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private UserRepository userRepository;
  @Autowired private ParticipationRepository participationRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AdminAccountInitializer adminAccountInitializer;

  private String anaToken;
  private String bobToken;
  private String carlaToken;
  private String danaToken;
  private String adminToken;
  private long anaId;
  private long bobId;
  private long carlaId;
  private long danaId;
  private long tripId;
  private String participants;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "https://localhost";
    RestAssured.port = port;
    RestAssured.useRelaxedHTTPSValidation();
    databaseCleaner.clear();
    adminAccountInitializer.run(new DefaultApplicationArguments());
    anaId = newUser("Ana", "ana@example.com");
    bobId = newUser("Bob", "bob@example.com");
    carlaId = newUser("Carla", "carla@example.com");
    danaId = newUser("Dana", "dana@example.com");
    anaToken = tokenOf("ana@example.com", PASSWORD);
    bobToken = tokenOf("bob@example.com", PASSWORD);
    carlaToken = tokenOf("carla@example.com", PASSWORD);
    danaToken = tokenOf("dana@example.com", PASSWORD);
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
    participants = TRIPS + "/" + tripId + "/participants";
  }

  @AfterEach
  void tearDown() {
    RestAssured.reset();
  }

  // --- helpers ---

  private long newUser(String name, String email) {
    return userRepository
        .save(new User(name, email, passwordEncoder.encode(PASSWORD), Role.USER))
        .getId();
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

  private static RequestSpecification json(String token) {
    return given().cookie("AuthToken", token).contentType(ContentType.JSON);
  }

  /** Ana invites the email; the id of the invitation is returned. */
  private long invite(String email) {
    return json(anaToken)
        .body("{\"email\":\"%s\"}".formatted(email))
        .post(TRIPS + "/" + tripId + "/invitations")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private long join(String token, String email) {
    long id = invite(email);
    json(token).body(ACCEPTED).put(RECEIVED + "/" + id + "/status").then().statusCode(200);
    return id;
  }

  private Response remove(String token, long userId) {
    return json(token).delete(participants + "/" + userId);
  }

  // --- listing ---

  @Test
  void theCreatorListsTheParticipantsWithTheirEmails() {
    join(bobToken, "bob@example.com");
    join(carlaToken, "carla@example.com");

    json(anaToken)
        .get(participants)
        .then()
        .statusCode(200)
        .body("content", hasSize(2))
        .body("totalElements", equalTo(2))
        .body("last", equalTo(true))
        .body("content[0].userId", equalTo((int) bobId))
        .body("content[0].name", equalTo("Bob"))
        .body("content[0].email", equalTo("bob@example.com"))
        .body("content[0].joinedAt", notNullValue())
        .body("content[0].avatarId", nullValue())
        .body("content[1].userId", equalTo((int) carlaId))
        .body("content.userId", not(hasItem((int) anaId)));
  }

  @Test
  void aParticipantSeesWhoElseIsInTheTripButNoEmails() {
    join(bobToken, "bob@example.com");
    join(carlaToken, "carla@example.com");

    Response response = json(bobToken).get(participants);

    response
        .then()
        .statusCode(200)
        .body("content", hasSize(2))
        .body("content[0].name", equalTo("Bob"))
        .body("content[0].email", nullValue())
        .body("content[1].name", equalTo("Carla"))
        .body("content[1].email", nullValue());
    assertThat(response.asString()).doesNotContain("@example.com");
  }

  @Test
  void theAvatarIdOfAParticipantIsTheUuidReturnedByTheAvatarUpload() {
    join(bobToken, "bob@example.com");
    String uploaded =
        json(bobToken)
            .contentType(ContentType.MULTIPART)
            .multiPart("file", "picture", ImageTestData.png(100), "image/png")
            .put("/api/v1/users/" + bobId + "/avatar")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getString("id");

    json(anaToken)
        .get(participants)
        .then()
        .statusCode(200)
        .body("content[0].avatarId", equalTo(uploaded));
    json(carlaToken).get(participants).then().statusCode(403);
    json(anaToken).get("/api/v1/images/" + uploaded).then().statusCode(200);
  }

  @Test
  void aTripWithoutParticipantsListsNobodyAndTheAdministratorMayRead() {
    json(anaToken).get(participants).then().statusCode(200).body("content", hasSize(0));
    join(bobToken, "bob@example.com");

    json(adminToken)
        .get(participants)
        .then()
        .statusCode(200)
        .body("content", hasSize(1))
        .body("content[0].email", nullValue());
  }

  @Test
  void aStrangerGets403WhenListing() {
    join(bobToken, "bob@example.com");

    json(carlaToken).get(participants).then().statusCode(403);
  }

  @Test
  void listingRefusesToSortByAnEmailOrAnInvalidId() {
    json(anaToken).queryParam("sort", "email").get(participants).then().statusCode(400);
    json(anaToken).get(TRIPS + "/abc/participants").then().statusCode(400);
  }

  // --- removing and leaving ---

  @Test
  void theCreatorRemovesAParticipantWhoLosesAccessAtOnce() {
    join(bobToken, "bob@example.com");
    json(bobToken).get(TRIPS + "/" + tripId).then().statusCode(200);
    json(bobToken).get(TRIPS).then().body("content", hasSize(1));

    remove(anaToken, bobId).then().statusCode(204);

    json(bobToken).get(TRIPS + "/" + tripId).then().statusCode(403);
    json(bobToken).get(participants).then().statusCode(403);
    json(bobToken).get(TRIPS).then().statusCode(200).body("content", hasSize(0));
    json(anaToken).get(participants).then().statusCode(200).body("content", hasSize(0));
    assertThat(participationRepository.count()).isZero();
  }

  @Test
  void aParticipantLeavesAndTheTripDisappearsFromTheirTrips() {
    join(bobToken, "bob@example.com");
    join(carlaToken, "carla@example.com");

    remove(bobToken, bobId).then().statusCode(204);

    json(bobToken).get(TRIPS + "/" + tripId).then().statusCode(403);
    json(bobToken).get(TRIPS).then().statusCode(200).body("content", hasSize(0));
    json(anaToken)
        .get(participants)
        .then()
        .statusCode(200)
        .body("content", hasSize(1))
        .body("content[0].name", equalTo("Carla"));
    json(carlaToken).get(TRIPS + "/" + tripId).then().statusCode(200);
  }

  @Test
  void theInvitationOfARemovedParticipantStaysAcceptedAsHistory() {
    long invitation = join(bobToken, "bob@example.com");

    remove(anaToken, bobId).then().statusCode(204);

    json(anaToken)
        .get(TRIPS + "/" + tripId + "/invitations/" + invitation)
        .then()
        .statusCode(200)
        .body("status", equalTo("ACCEPTED"));
  }

  @Test
  void aRemovedUserCanBeInvitedAgainAndJoinsAgain() {
    join(bobToken, "bob@example.com");
    remove(anaToken, bobId).then().statusCode(204);
    json(bobToken).get(TRIPS + "/" + tripId).then().statusCode(403);

    join(bobToken, "bob@example.com");

    json(bobToken).get(TRIPS + "/" + tripId).then().statusCode(200);
    json(anaToken).get(participants).then().body("content", hasSize(1));
  }

  @Test
  void aParticipantCannotRemoveSomeoneElseAndNothingChanges() {
    join(bobToken, "bob@example.com");
    join(carlaToken, "carla@example.com");

    remove(bobToken, carlaId).then().statusCode(403);
    remove(carlaToken, bobId).then().statusCode(403);

    assertThat(participationRepository.count()).isEqualTo(2);
  }

  @Test
  void aParticipantCannotRemoveTheCreator() {
    join(bobToken, "bob@example.com");

    remove(bobToken, anaId).then().statusCode(403);
  }

  @Test
  void theCreatorCannotLeaveTheirOwnTripWith409() {
    join(bobToken, "bob@example.com");

    remove(anaToken, anaId)
        .then()
        .statusCode(409)
        .body("status", equalTo(409))
        .body("message", equalTo("The creator cannot leave the trip; delete it instead"));

    json(anaToken).get(TRIPS + "/" + tripId).then().statusCode(200);
    assertThat(participationRepository.count()).isEqualTo(1);
  }

  @Test
  void theAdministratorCannotRemoveParticipants() {
    join(bobToken, "bob@example.com");

    remove(adminToken, bobId).then().statusCode(403);

    assertThat(participationRepository.count()).isEqualTo(1);
  }

  @Test
  void aStrangerAndAnAlreadyRemovedUserGet403() {
    join(bobToken, "bob@example.com");
    remove(anaToken, bobId).then().statusCode(204);

    remove(danaToken, danaId).then().statusCode(403);
    remove(danaToken, bobId).then().statusCode(403);
    remove(bobToken, bobId).then().statusCode(403);
  }

  @Test
  void anonymousRequestsAre401() {
    join(bobToken, "bob@example.com");

    given().get(participants).then().statusCode(401);
    given().delete(participants + "/" + bobId).then().statusCode(401);
    assertThat(participationRepository.count()).isEqualTo(1);
  }

  @Test
  void anUnknownTripIs404ForListingAndRemoving() {
    json(anaToken).get(TRIPS + "/999999/participants").then().statusCode(404);
    json(anaToken).delete(TRIPS + "/999999/participants/" + bobId).then().statusCode(404);
    json(bobToken).delete(TRIPS + "/999999/participants/" + bobId).then().statusCode(404);
  }

  @Test
  void removingSomeoneWhoIsNotAParticipantOrDoesNotExistIs404ForTheCreator() {
    join(bobToken, "bob@example.com");

    remove(anaToken, carlaId).then().statusCode(404);
    remove(anaToken, 999999).then().statusCode(404);
    remove(anaToken, bobId).then().statusCode(204);
    remove(anaToken, bobId).then().statusCode(404);
  }

  @Test
  void removingWithAnInvalidUserIdIs400() {
    json(anaToken).delete(participants + "/abc").then().statusCode(400);
  }

  @Test
  void deletingTheTripStillDeletesItsParticipantsAndTheyGet404() {
    join(bobToken, "bob@example.com");

    json(anaToken).delete(TRIPS + "/" + tripId).then().statusCode(204);

    json(bobToken).get(participants).then().statusCode(404);
    assertThat(participationRepository.count()).isZero();
  }
}
