package com.ruben.backend.user.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AdminAccountInitializer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

@Tag("B2")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class UserApiE2ETest {

  private static final String USERS = "/api/v1/users";
  private static final String LOGIN = "/api/v1/auth/login";
  private static final String ME = "/api/v1/auth/me";
  private static final String ADMIN_EMAIL = "admin@test.tripmate.com";

  @LocalServerPort private int port;

  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private UserRepository userRepository;
  @Autowired private AdminAccountInitializer adminAccountInitializer;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "https://localhost";
    RestAssured.port = port;
    RestAssured.useRelaxedHTTPSValidation();

    databaseCleaner.clear();
    adminAccountInitializer.run(new DefaultApplicationArguments());
  }

  @AfterEach
  void tearDown() {
    RestAssured.reset();
  }

  private static Response register(String name, String email, String password) {
    return given()
        .contentType(ContentType.JSON)
        .body(
            "{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}"
                .formatted(name, email, password))
        .post(USERS);
  }

  private static Response login(String email, String password) {
    return given()
        .contentType(ContentType.JSON)
        .body("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password))
        .post(LOGIN);
  }

  @Test
  void registerCreatesARegularUserWithoutExposingThePassword() {
    register("Ana", "ana@example.com", "secret-pass")
        .then()
        .statusCode(201)
        .header(
            "Location",
            endsWith(
                "/api/v1/users/"
                    + userRepository.findByEmail("ana@example.com").orElseThrow().getId()))
        .body("name", equalTo("Ana"))
        .body("email", equalTo("ana@example.com"))
        .body("role", equalTo("USER"))
        .body("id", notNullValue())
        .body("password", nullValue());
  }

  @Test
  void aRoleSentInTheBodyIsIgnored() {
    given()
        .contentType(ContentType.JSON)
        .body(
            "{\"name\":\"Eve\",\"email\":\"eve@example.com\",\"password\":\"secret-pass\","
                + "\"role\":\"ADMIN\"}")
        .post(USERS)
        .then()
        .statusCode(201)
        .body("role", equalTo("USER"));
  }

  @Test
  void theNewUserCanLogInAndIsIdentifiedByMe() {
    register("Ana", "ana@example.com", "secret-pass").then().statusCode(201);

    String token =
        login("ana@example.com", "secret-pass")
            .then()
            .statusCode(200)
            .extract()
            .cookie("AuthToken");

    given()
        .cookie("AuthToken", token)
        .get(ME)
        .then()
        .statusCode(200)
        .body("email", equalTo("ana@example.com"));
  }

  @Test
  void loginWithAWrongPasswordAfterRegisteringIsRejected() {
    register("Ana", "ana@example.com", "secret-pass").then().statusCode(201);

    login("ana@example.com", "other-pass").then().statusCode(401);
  }

  @Test
  void aRegisteredEmailCannotBeRegisteredAgainInAnyCase() {
    register("Ana", "ana@example.com", "secret-pass").then().statusCode(201);

    register("Other", "ANA@example.com", "secret-pass")
        .then()
        .statusCode(409)
        .body("status", equalTo(409))
        .body("message", equalTo("Email is already registered"))
        .body("path", equalTo(USERS));
  }

  @Test
  void theAdministratorEmailCannotBeRegistered() {
    register("Mallory", ADMIN_EMAIL, "secret-pass").then().statusCode(409);
  }

  @Test
  void invalidDataIsRejectedWithAnErrorResponse() {
    register("", "ana@example.com", "secret-pass")
        .then()
        .statusCode(400)
        .body("status", equalTo(400));
    register("Ana", "not-an-email", "secret-pass").then().statusCode(400);
    register("Ana", "ana@example.com", "short").then().statusCode(400);
  }

  @Test
  void malformedJsonIsRejectedWithAnErrorResponse() {
    given()
        .contentType(ContentType.JSON)
        .body("{not json")
        .post(USERS)
        .then()
        .statusCode(400)
        .body("message", equalTo("Malformed JSON request"));
  }

  @Test
  void meIsStillRejectedWhenNotLoggedIn() {
    given().get(ME).then().statusCode(401);
  }
}
