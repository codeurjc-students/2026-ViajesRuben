package com.ruben.backend.invitation.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.invitation.repository.InvitationRepository;
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

@Tag("I2")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class InvitationResponseApiE2ETest {

  private static final String PASSWORD = "secret-pass";
  private static final String TRIPS = "/api/v1/trips";
  private static final String RECEIVED = "/api/v1/invitations";
  private static final String TRIP_BODY =
      "{\"name\":\"Rome\",\"destination\":\"Rome\",\"startDate\":\"2027-07-10\","
          + "\"endDate\":\"2027-07-14\"}";
  private static final String ACTIVITY_BODY = "{\"name\":\"Colosseum\",\"date\":\"2027-07-11\"}";
  private static final String ACCEPTED = "{\"status\":\"ACCEPTED\"}";
  private static final String REJECTED = "{\"status\":\"REJECTED\"}";

  @LocalServerPort private int port;

  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private UserRepository userRepository;
  @Autowired private InvitationRepository invitationRepository;
  @Autowired private ParticipationRepository participationRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AdminAccountInitializer adminAccountInitializer;

  private String anaToken;
  private String bobToken;
  private String carlaToken;
  private String adminToken;
  private long tripId;
  private String invitations;

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
    tripId = createTrip(anaToken);
    invitations = TRIPS + "/" + tripId + "/invitations";
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

  private static RequestSpecification json(String token) {
    return given().cookie("AuthToken", token).contentType(ContentType.JSON);
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

  /** Ana invites the email; the id of the invitation is returned. */
  private long invite(String email) {
    return json(anaToken)
        .body("{\"email\":\"%s\"}".formatted(email))
        .post(invitations)
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private Response respond(String token, long id, String body) {
    return json(token).body(body).put(RECEIVED + "/" + id + "/status");
  }

  private String tokenOfInvitation(long id) {
    return invitationRepository.findById(id).orElseThrow().getToken();
  }

  /** Bob is invited and accepts; the invitation id is returned. */
  private long bobJoins() {
    long id = invite("bob@example.com");
    respond(bobToken, id, ACCEPTED).then().statusCode(200);
    return id;
  }

  // --- accept and reject ---

  @Test
  void theInvitedUserAcceptsAndTheAnswerShowsTheTripButNoPrivateData() {
    long id = invite("bob@example.com");

    Response response = respond(bobToken, id, ACCEPTED);

    response
        .then()
        .statusCode(200)
        .body("id", equalTo((int) id))
        .body("tripId", equalTo((int) tripId))
        .body("tripName", equalTo("Rome"))
        .body("inviterName", equalTo("Ana"))
        .body("status", equalTo("ACCEPTED"))
        .body("respondedAt", notNullValue())
        .body("token", nullValue())
        .body("invitedEmail", nullValue());
    assertThat(response.asString()).doesNotContain(tokenOfInvitation(id));
    assertThat(participationRepository.count()).isEqualTo(1);
  }

  @Test
  void theInvitedUserRejectsAndNothingIsShared() {
    long id = invite("bob@example.com");

    respond(bobToken, id, REJECTED).then().statusCode(200).body("status", equalTo("REJECTED"));

    assertThat(participationRepository.count()).isZero();
    json(bobToken).get(TRIPS + "/" + tripId).then().statusCode(403);
    json(bobToken).get(TRIPS).then().statusCode(200).body("content", hasSize(0));
  }

  @Test
  void theCreatorsInvitationListShowsTheNewStatuses() {
    long accepted = invite("bob@example.com");
    long rejected = invite("carla@example.com");
    respond(bobToken, accepted, ACCEPTED).then().statusCode(200);
    respond(carlaToken, rejected, REJECTED).then().statusCode(200);

    json(anaToken)
        .queryParam("sort", "id,asc")
        .get(invitations)
        .then()
        .statusCode(200)
        .body("content[0].status", equalTo("ACCEPTED"))
        .body("content[0].respondedAt", notNullValue())
        .body("content[1].status", equalTo("REJECTED"));
  }

  @Test
  void respondingTwiceIs409AndTheFirstAnswerStands() {
    long id = invite("bob@example.com");
    respond(bobToken, id, REJECTED).then().statusCode(200);

    respond(bobToken, id, ACCEPTED).then().statusCode(409).body("status", equalTo(409));
    respond(bobToken, id, REJECTED).then().statusCode(409);

    assertThat(participationRepository.count()).isZero();
  }

  @Test
  void invitingSomeoneWhoAlreadyParticipatesIs409() {
    bobJoins();

    json(anaToken)
        .body("{\"email\":\"Bob@Example.com\"}")
        .post(invitations)
        .then()
        .statusCode(409)
        .body("message", containsString("participant"));
  }

  @Test
  void aNewInvitationAfterARejectionIsAccepted() {
    long id = invite("bob@example.com");
    respond(bobToken, id, REJECTED).then().statusCode(200);

    long again = invite("bob@example.com");
    respond(bobToken, again, ACCEPTED).then().statusCode(200);

    assertThat(participationRepository.count()).isEqualTo(1);
  }

  @Test
  void aBadAnswerIs400() {
    long id = invite("bob@example.com");

    respond(bobToken, id, "{\"status\":\"PENDING\"}").then().statusCode(400);
    respond(bobToken, id, "{\"status\":\"MAYBE\"}").then().statusCode(400);
    respond(bobToken, id, "{}").then().statusCode(400);

    assertThat(participationRepository.count()).isZero();
  }

  // --- who may answer ---

  @Test
  void anotherUserTheCreatorAndTheAdministratorGet403AndNothingChanges() {
    long id = invite("bob@example.com");

    respond(carlaToken, id, ACCEPTED).then().statusCode(403);
    respond(anaToken, id, ACCEPTED).then().statusCode(403);
    respond(adminToken, id, ACCEPTED).then().statusCode(403);

    assertThat(participationRepository.count()).isZero();
    json(anaToken)
        .get(invitations + "/" + id)
        .then()
        .statusCode(200)
        .body("status", equalTo("PENDING"));
  }

  @Test
  void anonymousRequestsAre401() {
    long id = invite("bob@example.com");

    given()
        .contentType(ContentType.JSON)
        .body(ACCEPTED)
        .put(RECEIVED + "/" + id + "/status")
        .then()
        .statusCode(401);
    given().get(RECEIVED).then().statusCode(401);
    given().get(RECEIVED + "/token/" + tokenOfInvitation(id)).then().statusCode(401);
    assertThat(participationRepository.count()).isZero();
  }

  @Test
  void anUnknownInvitationOrTokenIs404() {
    respond(bobToken, 999999, ACCEPTED).then().statusCode(404);
    json(bobToken).get(RECEIVED + "/token/does-not-exist").then().statusCode(404);
  }

  @Test
  void anInvitationOfADeletedTripIs404() {
    long id = invite("bob@example.com");
    String token = tokenOfInvitation(id);
    json(anaToken).delete(TRIPS + "/" + tripId).then().statusCode(204);

    respond(bobToken, id, ACCEPTED).then().statusCode(404);
    json(bobToken).get(RECEIVED + "/token/" + token).then().statusCode(404);
    json(bobToken).get(RECEIVED).then().statusCode(200).body("content", hasSize(0));
  }

  // --- by token ---

  @Test
  void theTokenOfTheEmailLinkShowsTheInvitationToItsAddresseeInAnyStatus() {
    long id = invite("bob@example.com");
    String token = tokenOfInvitation(id);

    json(bobToken)
        .get(RECEIVED + "/token/" + token)
        .then()
        .statusCode(200)
        .body("tripName", equalTo("Rome"))
        .body("inviterName", equalTo("Ana"))
        .body("status", equalTo("PENDING"))
        .body("token", nullValue());
    respond(bobToken, id, REJECTED).then().statusCode(200);
    json(bobToken)
        .get(RECEIVED + "/token/" + token)
        .then()
        .statusCode(200)
        .body("status", equalTo("REJECTED"));
  }

  @Test
  void theTokenRevealsNothingToAnotherUser() {
    String token = tokenOfInvitation(invite("bob@example.com"));

    Response other = json(carlaToken).get(RECEIVED + "/token/" + token);

    other.then().statusCode(403);
    assertThat(other.asString()).doesNotContain("Rome").doesNotContain("Ana");
  }

  // --- my pending invitations ---

  @Test
  void myInvitationsListsOnlyMyPendingOnesPaginatedAndAnsweredOnesLeaveTheList() {
    for (int i = 1; i <= 11; i++) {
      long other = createTrip(anaToken);
      json(anaToken)
          .body("{\"email\":\"bob@example.com\"}")
          .post(TRIPS + "/" + other + "/invitations")
          .then()
          .statusCode(201);
    }
    long forCarla = invite("carla@example.com");

    Response first = json(bobToken).get(RECEIVED);
    first
        .then()
        .statusCode(200)
        .body("content", hasSize(10))
        .body("totalElements", equalTo(11))
        .body("last", equalTo(false))
        .body("content[0].inviterName", equalTo("Ana"))
        .body("content[0].token", nullValue())
        .body("content.id", not(hasItem((int) forCarla)));
    json(bobToken)
        .queryParam("page", 1)
        .get(RECEIVED)
        .then()
        .statusCode(200)
        .body("content", hasSize(1))
        .body("last", equalTo(true));

    respond(bobToken, first.jsonPath().getLong("content[0].id"), REJECTED).then().statusCode(200);
    json(bobToken).get(RECEIVED).then().body("totalElements", equalTo(10));
  }

  @Test
  void myInvitationsIsEmptyWithoutInvitationsAndRefusesToSortByAnEmail() {
    json(bobToken).get(RECEIVED).then().statusCode(200).body("content", hasSize(0));
    json(bobToken).queryParam("sort", "invitedEmail").get(RECEIVED).then().statusCode(400);
  }

  @Test
  void myInvitationsMatchesTheEmailIgnoringCase() {
    userRepository.save(
        new User("Dana", "Dana@Example.com", passwordEncoder.encode(PASSWORD), Role.USER));
    invite("dana@example.com");

    json(tokenOf("Dana@Example.com", PASSWORD))
        .get(RECEIVED)
        .then()
        .statusCode(200)
        .body("content", hasSize(1));
  }

  // --- what a participant can and cannot do ---

  @Test
  void aParticipantReadsTheTripItsActivitiesItsCoverStateAndSeesItInMyTrips() {
    json(anaToken)
        .body(ACTIVITY_BODY)
        .post(TRIPS + "/" + tripId + "/activities")
        .then()
        .statusCode(201);
    json(bobToken).get(TRIPS + "/" + tripId).then().statusCode(403);
    bobJoins();

    json(bobToken).get(TRIPS + "/" + tripId).then().statusCode(200).body("name", equalTo("Rome"));
    json(bobToken)
        .get(TRIPS + "/" + tripId + "/activities")
        .then()
        .statusCode(200)
        .body("content", hasSize(1))
        .body("content[0].name", equalTo("Colosseum"));
    json(bobToken).get(TRIPS + "/" + tripId + "/activities/cost").then().statusCode(200);
    json(bobToken).get(TRIPS + "/" + tripId + "/conflicts").then().statusCode(200);
    json(bobToken)
        .get(TRIPS)
        .then()
        .statusCode(200)
        .body("content", hasSize(1))
        .body("content[0].id", equalTo((int) tripId));
  }

  /** The itinerary is the exception: participants change its activities (I4). */
  @Test
  void aParticipantCannotChangeTheTripItself() {
    bobJoins();
    String trip = TRIPS + "/" + tripId;

    json(bobToken).body(TRIP_BODY).put(trip).then().statusCode(403);
    json(bobToken).body("{\"published\":true}").put(trip + "/publication").then().statusCode(403);
    json(bobToken).delete(trip).then().statusCode(403);
    json(bobToken).delete(trip + "/cover").then().statusCode(403);
    json(bobToken)
        .body("{\"email\":\"carla@example.com\"}")
        .post(trip + "/invitations")
        .then()
        .statusCode(403);
    json(bobToken).get(trip + "/invitations").then().statusCode(403);

    json(anaToken).get(trip).then().statusCode(200).body("published", equalTo(false));
  }

  @Test
  void aParticipantDoesNotSeeTheTripsOfOtherUsers() {
    bobJoins();
    long secret = createTrip(carlaToken);

    json(bobToken).get(TRIPS + "/" + secret).then().statusCode(403);
    json(bobToken).get(TRIPS).then().body("content.id", not(hasItem((int) secret)));
  }

  // --- deleting the trip ---

  @Test
  void deletingTheTripDeletesParticipationsAndInvitationsAndTheParticipantLosesAccess() {
    bobJoins();
    invite("carla@example.com");
    assertThat(participationRepository.count()).isEqualTo(1);

    json(anaToken).delete(TRIPS + "/" + tripId).then().statusCode(204);

    assertThat(participationRepository.count()).isZero();
    assertThat(invitationRepository.count()).isZero();
    json(bobToken).get(TRIPS + "/" + tripId).then().statusCode(404);
    json(bobToken).get(TRIPS).then().body("content", hasSize(0));
  }

  @Test
  void theAdministratorDeletingTheTripDeletesItsParticipationsToo() {
    bobJoins();

    json(adminToken).delete(TRIPS + "/" + tripId).then().statusCode(204);

    assertThat(participationRepository.count()).isZero();
  }
}
