package com.ruben.backend.user.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.common.security.AdminProperties;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AdminAccountInitializer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.UUID;
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

@Tag("B2")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AuthApiE2ETest {

  private static final String LOGIN = "/api/v1/auth/login";
  private static final String LOGOUT = "/api/v1/auth/logout";
  private static final String ME = "/api/v1/auth/me";
  private static final String ADMIN_AREA = "/api/v1/admin/anything";

  /** Matches the BCrypt hash configured for the test administrator. */
  private static final String ADMIN_PASSWORD = "Admin-test-1";

  private static final String USER_EMAIL = "ana@example.com";
  private static final String BLOCKED_EMAIL = "blocked@example.com";
  private static final String USER_PASSWORD = "User-pass-1";

  @LocalServerPort private int port;

  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AdminAccountInitializer adminAccountInitializer;
  @Autowired private AdminProperties adminProperties;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "https://localhost";
    RestAssured.port = port;
    RestAssured.useRelaxedHTTPSValidation();

    databaseCleaner.clear();
    adminAccountInitializer.run(new DefaultApplicationArguments());
    userRepository.save(
        new User("Ana", USER_EMAIL, passwordEncoder.encode(USER_PASSWORD), Role.USER));
    User blocked =
        new User("Blocked", BLOCKED_EMAIL, passwordEncoder.encode(USER_PASSWORD), Role.USER);
    blocked.setStatus(UserStatus.BLOCKED);
    userRepository.save(blocked);
  }

  @AfterEach
  void tearDown() {
    RestAssured.reset();
  }

  private static Response login(String email, String password) {
    return given()
        .contentType(ContentType.JSON)
        .body("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password))
        .post(LOGIN);
  }

  private static String tokenOf(String email, String password) {
    Response response = login(email, password);
    response.then().statusCode(200);
    return response.getCookie("AuthToken");
  }

  private static void assertCookieCleared(Response response) {
    String setCookie = response.getHeader("Set-Cookie");
    assertThat(setCookie).startsWith("AuthToken=;").contains("Max-Age=0");
  }

  @Test
  void theAdministratorLogsInAndReceivesAnHttpOnlySecureCookie() {
    Response response = login(adminProperties.email(), ADMIN_PASSWORD);

    response
        .then()
        .statusCode(200)
        .body("role", equalTo("ADMIN"))
        .body("email", equalTo(adminProperties.email()))
        .body("id", notNullValue())
        .body("password", equalTo(null));
    assertThat(response.getCookie("AuthToken")).isNotBlank();
    assertThat(response.getHeader("Set-Cookie"))
        .contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api");
  }

  @Test
  void aRegularUserLogsInWithTheUserRole() {
    login(USER_EMAIL, USER_PASSWORD)
        .then()
        .statusCode(200)
        .body("role", equalTo("USER"))
        .body("name", equalTo("Ana"));
  }

  @Test
  void meReturnsTheUserOfTheCookie() {
    String token = tokenOf(USER_EMAIL, USER_PASSWORD);

    given()
        .cookie("AuthToken", token)
        .get(ME)
        .then()
        .statusCode(200)
        .body("email", equalTo(USER_EMAIL))
        .body("role", equalTo("USER"));
  }

  @Test
  void meWithoutCookieReturns401WithTheErrorBody() {
    given()
        .get(ME)
        .then()
        .statusCode(401)
        .body("status", equalTo(401))
        .body("error", equalTo("Unauthorized"))
        .body("message", equalTo("Authentication is required"))
        .body("path", equalTo(ME))
        .body("timestamp", notNullValue());
  }

  @Test
  void aWrongPasswordReturns401() {
    login(USER_EMAIL, "wrong")
        .then()
        .statusCode(401)
        .body("message", equalTo("Invalid email or password"));
  }

  @Test
  void anUnknownEmailReturnsTheSame401AsAWrongPassword() {
    login("ghost@example.com", USER_PASSWORD)
        .then()
        .statusCode(401)
        .body("message", equalTo("Invalid email or password"));
  }

  @Test
  void aBlockedUserWithTheRightPasswordReturns403AndNoCookie() {
    Response response = login(BLOCKED_EMAIL, USER_PASSWORD);

    response.then().statusCode(403).body("message", equalTo("Account is blocked"));
    assertThat(response.getHeader("Set-Cookie")).isNull();
  }

  @Test
  void aBlockedUserWithTheWrongPasswordGetsTheGeneric401() {
    login(BLOCKED_EMAIL, "wrong")
        .then()
        .statusCode(401)
        .body("message", equalTo("Invalid email or password"));
  }

  @Test
  void aMalformedLoginBodyReturns400ListingTheInvalidFields() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"email\":\"not-an-email\",\"password\":\"\"}")
        .post(LOGIN)
        .then()
        .statusCode(400)
        .body("status", equalTo(400))
        .body(
            "message",
            equalTo("email: must be a well-formed email address; password: must not be blank"));
  }

  @Test
  void aUserBlockedAfterLoggingInIsRejectedImmediatelyAndTheCookieIsCleared() {
    String token = tokenOf(USER_EMAIL, USER_PASSWORD);
    User ana = userRepository.findByEmail(USER_EMAIL).orElseThrow();
    ana.setStatus(UserStatus.BLOCKED);
    userRepository.save(ana);

    Response response = given().cookie("AuthToken", token).get(ME);

    response.then().statusCode(401).body("message", equalTo("Account is blocked"));
    assertCookieCleared(response);
  }

  @Test
  void aTokenOfADeletedUserIsRejected() {
    String token = tokenOf(USER_EMAIL, USER_PASSWORD);
    userRepository.delete(userRepository.findByEmail(USER_EMAIL).orElseThrow());

    Response response = given().cookie("AuthToken", token).get(ME);

    response.then().statusCode(401);
    assertCookieCleared(response);
  }

  @Test
  void aGarbageCookieReturns401AndIsCleared() {
    Response response = given().cookie("AuthToken", "not-a-jwt").get(ME);

    response.then().statusCode(401);
    assertCookieCleared(response);
  }

  @Test
  void aBadCookieOnAPublicEndpointFailsOnceAndIsClearedSoThePageWorksOnTheNextRequest() {
    String image = "/api/v1/images/" + UUID.randomUUID();
    Response rejected = given().cookie("AuthToken", "not-a-jwt").get(image);

    rejected.then().statusCode(401);
    assertCookieCleared(rejected);
    // Public again: the unknown image is a 404, no longer a 401.
    given().get(image).then().statusCode(404);
  }

  @Test
  void logoutReturns204AndClearsTheCookie() {
    Response response =
        given().cookie("AuthToken", tokenOf(USER_EMAIL, USER_PASSWORD)).post(LOGOUT);

    response.then().statusCode(204);
    assertCookieCleared(response);
  }

  @Test
  void theAdminAreaRejectsAnonymousWith401() {
    given().get(ADMIN_AREA).then().statusCode(401);
  }

  @Test
  void theAdminAreaRejectsARegularUserWith403() {
    given()
        .cookie("AuthToken", tokenOf(USER_EMAIL, USER_PASSWORD))
        .get(ADMIN_AREA)
        .then()
        .statusCode(403)
        .body("status", equalTo(403))
        .body("message", equalTo("Access is denied"));
  }

  @Test
  void theAdminAreaLetsTheAdministratorIn() {
    // No admin endpoint exists yet: a 404 (not 401/403) shows the request passed the security
    // rules.
    given()
        .cookie("AuthToken", tokenOf(adminProperties.email(), ADMIN_PASSWORD))
        .get(ADMIN_AREA)
        .then()
        .statusCode(404);
  }
}
