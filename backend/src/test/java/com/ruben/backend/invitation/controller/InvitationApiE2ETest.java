package com.ruben.backend.invitation.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.invitation.repository.InvitationRepository;
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

@Tag("I1")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class InvitationApiE2ETest {

  private static final String PASSWORD = "secret-pass";
  private static final String TRIPS = "/api/v1/trips";
  private static final String TRIP_BODY =
      "{\"name\":\"Rome\",\"destination\":\"Rome\",\"startDate\":\"2027-07-10\","
          + "\"endDate\":\"2027-07-14\"}";

  @LocalServerPort private int port;

  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private UserRepository userRepository;
  @Autowired private InvitationRepository invitationRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AdminAccountInitializer adminAccountInitializer;

  private String anaToken;
  private String bobToken;
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
    anaToken = tokenOf("ana@example.com", PASSWORD);
    bobToken = tokenOf("bob@example.com", PASSWORD);
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

  private static String body(String email) {
    return "{\"email\":\"%s\"}".formatted(email);
  }

  private long invite(String email) {
    return json(anaToken)
        .body(body(email))
        .post(invitations)
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  // --- create ---

  @Test
  void createAnswers201WithTheLocationAndNoTokenInTheBody() {
    Response response = json(anaToken).body(body("Friend@Example.com")).post(invitations);

    response
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("invitedEmail", equalTo("friend@example.com"))
        .body("status", equalTo("PENDING"))
        .body("sentAt", notNullValue())
        .body("token", nullValue());
    assertThat(response.getHeader("Location"))
        .endsWith(invitations + "/" + response.jsonPath().getLong("id"));
    assertThat(response.asString()).doesNotContain("token");
    assertThat(invitationRepository.findAll().getFirst().getToken())
        .isNotBlank()
        .satisfies(token -> assertThat(response.asString()).doesNotContain(token));
  }

  @Test
  void createFollowsTheLocationToTheInvitation() {
    String location =
        json(anaToken).body(body("friend@example.com")).post(invitations).getHeader("Location");

    json(anaToken)
        .get(location)
        .then()
        .statusCode(200)
        .body("invitedEmail", equalTo("friend@example.com"))
        .body("token", nullValue());
  }

  @Test
  void aSecondPendingInvitationForTheSameEmailIs409() {
    invite("friend@example.com");

    json(anaToken)
        .body(body("FRIEND@example.com"))
        .post(invitations)
        .then()
        .statusCode(409)
        .body("status", equalTo(409));
    assertThat(invitationRepository.count()).isEqualTo(1);
  }

  @Test
  void invitingTheCreatorsOwnEmailIs400() {
    json(anaToken).body(body("Ana@example.com")).post(invitations).then().statusCode(400);
    assertThat(invitationRepository.count()).isZero();
  }

  @Test
  void aMissingOrInvalidEmailIs400() {
    json(anaToken).body("{}").post(invitations).then().statusCode(400);
    json(anaToken).body(body("not-an-email")).post(invitations).then().statusCode(400);
    json(anaToken).body(body("")).post(invitations).then().statusCode(400);
  }

  @Test
  void anEmailWithoutAnAccountCanBeInvited() {
    json(anaToken).body(body("nobody@example.com")).post(invitations).then().statusCode(201);
  }

  @Test
  void anotherUserCannotCreateOrList() {
    json(bobToken).body(body("friend@example.com")).post(invitations).then().statusCode(403);
    json(bobToken).get(invitations).then().statusCode(403);
    assertThat(invitationRepository.count()).isZero();
  }

  @Test
  void theAdministratorCannotCreateOrList() {
    json(adminToken).body(body("friend@example.com")).post(invitations).then().statusCode(403);
    json(adminToken).get(invitations).then().statusCode(403);
  }

  @Test
  void anonymousRequestsAre401() {
    given()
        .contentType(ContentType.JSON)
        .body(body("friend@example.com"))
        .post(invitations)
        .then()
        .statusCode(401);
    given().get(invitations).then().statusCode(401);
    given().get(invitations + "/1").then().statusCode(401);
  }

  @Test
  void anUnknownTripIs404ForEveryOperation() {
    String unknown = TRIPS + "/999999/invitations";

    json(anaToken).body(body("friend@example.com")).post(unknown).then().statusCode(404);
    json(anaToken).get(unknown).then().statusCode(404);
    json(anaToken).get(unknown + "/1").then().statusCode(404);
  }

  @Test
  void anUnknownInvitationIs404() {
    json(anaToken).get(invitations + "/999999").then().statusCode(404);
  }

  // --- list ---

  @Test
  void listIsPaginatedNewestFirstAndNeverShowsTheToken() {
    for (int i = 1; i <= 12; i++) {
      invite("friend%02d@example.com".formatted(i));
    }

    json(anaToken)
        .get(invitations)
        .then()
        .statusCode(200)
        .body("content", hasSize(10))
        .body("totalElements", equalTo(12))
        .body("last", equalTo(false))
        .body("content[0].invitedEmail", equalTo("friend12@example.com"))
        .body("content[0].token", nullValue())
        .body("content[0].status", equalTo("PENDING"));
    json(anaToken)
        .queryParam("page", 1)
        .get(invitations)
        .then()
        .statusCode(200)
        .body("content", hasSize(2))
        .body("last", equalTo(true));
  }

  @Test
  void listOfATripWithoutInvitationsIsEmpty() {
    json(anaToken)
        .get(invitations)
        .then()
        .statusCode(200)
        .body("content", hasSize(0))
        .body("totalElements", equalTo(0));
  }

  @Test
  void listCanBeSortedAndRefusesAPropertyThatIsNotSortable() {
    invite("b@example.com");
    invite("a@example.com");

    json(anaToken)
        .queryParam("sort", "invitedEmail,asc")
        .get(invitations)
        .then()
        .statusCode(200)
        .body("content[0].invitedEmail", equalTo("a@example.com"));
    json(anaToken)
        .queryParam("sort", "token")
        .get(invitations)
        .then()
        .statusCode(400)
        .body("message", containsString("token"))
        .body("message", not(containsString("secret")));
  }

  // --- trip deletion ---

  @Test
  void deletingTheTripDeletesItsInvitations() {
    invite("friend@example.com");
    invite("other@example.com");
    assertThat(invitationRepository.count()).isEqualTo(2);

    json(anaToken).delete(TRIPS + "/" + tripId).then().statusCode(204);

    assertThat(invitationRepository.count()).isZero();
    json(anaToken).get(invitations).then().statusCode(404);
  }

  @Test
  void theAdministratorDeletingTheTripDeletesItsInvitationsToo() {
    invite("friend@example.com");

    json(adminToken).delete(TRIPS + "/" + tripId).then().statusCode(204);

    assertThat(invitationRepository.count()).isZero();
  }
}
