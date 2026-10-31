package com.ruben.backend.user.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
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

@Tag("B7")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AdminUserApiE2ETest {

  private static final String ADMIN_USERS = "/api/v1/admin/users";
  private static final String USERS = "/api/v1/users";
  private static final String LOGIN = "/api/v1/auth/login";
  private static final String ME = "/api/v1/auth/me";
  private static final String PASSWORD = "secret-pass";
  private static final String ADMIN_EMAIL = "admin@test.tripmate.com";
  private static final String ADMIN_PASSWORD = "Admin-test-1";

  @LocalServerPort private int port;

  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private AdminAccountInitializer adminAccountInitializer;

  private long anaId;
  private String anaToken;
  private long bobId;
  private String adminToken;
  private long adminId;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "https://localhost";
    RestAssured.port = port;
    RestAssured.useRelaxedHTTPSValidation();

    databaseCleaner.clear();
    adminAccountInitializer.run(new DefaultApplicationArguments());
    anaId = register("Ana Ruiz", "ana@example.com");
    anaToken = tokenOf("ana@example.com", PASSWORD);
    bobId = register("Bob", "bob@example.com");
    adminToken = tokenOf(ADMIN_EMAIL, ADMIN_PASSWORD);
    adminId = as(adminToken).get(ME).then().statusCode(200).extract().jsonPath().getLong("id");
  }

  @AfterEach
  void tearDown() {
    RestAssured.reset();
  }

  private static long register(String name, String email) {
    return given()
        .contentType(ContentType.JSON)
        .body(
            "{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}"
                .formatted(name, email, PASSWORD))
        .post(USERS)
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private static Response login(String email, String password) {
    return given()
        .contentType(ContentType.JSON)
        .body("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password))
        .post(LOGIN);
  }

  private static String tokenOf(String email, String password) {
    return login(email, password).then().statusCode(200).extract().cookie("AuthToken");
  }

  private static RequestSpecification as(String token) {
    return given().cookie("AuthToken", token);
  }

  private static Response edit(String token, long id, String name, String email) {
    return as(token)
        .contentType(ContentType.JSON)
        .body("{\"name\":\"%s\",\"email\":\"%s\"}".formatted(name, email))
        .put(ADMIN_USERS + "/" + id);
  }

  private static Response setStatus(String token, long id, String status) {
    return as(token)
        .contentType(ContentType.JSON)
        .body("{\"status\":\"%s\"}".formatted(status))
        .put(ADMIN_USERS + "/" + id + "/status");
  }

  // --- Who may call ---

  @Test
  void everyOperationRequiresLogin() {
    given().get(ADMIN_USERS).then().statusCode(401).body("status", equalTo(401));
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Ana\",\"email\":\"ana@example.com\"}")
        .put(ADMIN_USERS + "/" + anaId)
        .then()
        .statusCode(401);
    given()
        .contentType(ContentType.JSON)
        .body("{\"status\":\"BLOCKED\"}")
        .put(ADMIN_USERS + "/" + anaId + "/status")
        .then()
        .statusCode(401);
  }

  @Test
  void aRegularUserGets403OnEveryOperation() {
    as(anaToken).get(ADMIN_USERS).then().statusCode(403).body("status", equalTo(403));
    edit(anaToken, bobId, "Bob", "bob@example.com").then().statusCode(403);
    setStatus(anaToken, bobId, "BLOCKED").then().statusCode(403);
    // Nothing happened to Bob.
    login("bob@example.com", PASSWORD).then().statusCode(200);
  }

  // --- List and search ---

  @Test
  void theAdministratorListsTheRegularUsersWithoutSecrets() {
    as(adminToken)
        .get(ADMIN_USERS)
        .then()
        .statusCode(200)
        .body("content.name", contains("Ana Ruiz", "Bob"))
        .body("content[0].email", equalTo("ana@example.com"))
        .body("content[0].status", equalTo("ACTIVE"))
        .body("content[0].registrationDate", not(nullValue()))
        .body("content[0].password", nullValue())
        .body("content[0].role", nullValue())
        .body("totalElements", equalTo(2))
        .body("last", equalTo(true));
  }

  @Test
  void searchMatchesNameOrEmailIgnoringCase() {
    as(adminToken)
        .queryParam("search", "RUIZ")
        .get(ADMIN_USERS)
        .then()
        .statusCode(200)
        .body("content.name", contains("Ana Ruiz"));
    as(adminToken)
        .queryParam("search", "BOB@exam")
        .get(ADMIN_USERS)
        .then()
        .body("content.name", contains("Bob"));
    as(adminToken)
        .queryParam("search", "nobody")
        .get(ADMIN_USERS)
        .then()
        .body("content", hasSize(0))
        .body("totalElements", equalTo(0));
  }

  @Test
  void theListIsPaginated() {
    as(adminToken)
        .queryParam("size", 1)
        .get(ADMIN_USERS)
        .then()
        .statusCode(200)
        .body("content.name", contains("Ana Ruiz"))
        .body("last", equalTo(false))
        .body("totalElements", equalTo(2));
    as(adminToken)
        .queryParams("size", 1, "page", 1)
        .get(ADMIN_USERS)
        .then()
        .body("content.name", contains("Bob"))
        .body("last", equalTo(true));
  }

  @Test
  void sortingByAFieldThatIsNotSortableIs400() {
    as(adminToken).queryParam("sort", "password").get(ADMIN_USERS).then().statusCode(400);
  }

  // --- Edit ---

  @Test
  void editChangesTheNameAndTheEmailAndTheUserLogsInWithTheNewOne() {
    edit(adminToken, anaId, " Ana R ", "Ana.R@Example.com")
        .then()
        .statusCode(200)
        .body("name", equalTo("Ana R"))
        .body("email", equalTo("ana.r@example.com"));

    login("ana.r@example.com", PASSWORD).then().statusCode(200);
    login("ana@example.com", PASSWORD).then().statusCode(401);
  }

  @Test
  void editWithATakenOrAdministratorEmailIs409() {
    edit(adminToken, anaId, "Ana", "bob@example.com").then().statusCode(409);
    edit(adminToken, anaId, "Ana", ADMIN_EMAIL).then().statusCode(409);
  }

  @Test
  void editWithInvalidDataIs400AndAMissingUserIs404() {
    edit(adminToken, anaId, "", "ana@example.com").then().statusCode(400);
    edit(adminToken, anaId, "Ana", "not-an-email").then().statusCode(400);
    edit(adminToken, 999999, "Ana", "ana@example.com").then().statusCode(404);
  }

  // --- Block and unblock ---

  @Test
  void blockingStopsLoginAndTheOpenSessionAndUnblockingRestoresBoth() {
    setStatus(adminToken, anaId, "BLOCKED")
        .then()
        .statusCode(200)
        .body("status", equalTo("BLOCKED"));

    login("ana@example.com", PASSWORD).then().statusCode(403);
    as(anaToken).get(ME).then().statusCode(401);
    as(adminToken)
        .queryParam("search", "ana")
        .get(ADMIN_USERS)
        .then()
        .body("content[0].status", equalTo("BLOCKED"));

    setStatus(adminToken, anaId, "ACTIVE").then().statusCode(200).body("status", equalTo("ACTIVE"));

    String freshToken = tokenOf("ana@example.com", PASSWORD);
    as(freshToken).get(ME).then().statusCode(200).body("id", equalTo((int) anaId));
  }

  @Test
  void settingTheStatusTheUserAlreadyHasIsHarmless() {
    setStatus(adminToken, anaId, "ACTIVE").then().statusCode(200).body("status", equalTo("ACTIVE"));
    as(anaToken).get(ME).then().statusCode(200);
  }

  @Test
  void anInvalidStatusIs400AndAMissingUserIs404() {
    setStatus(adminToken, anaId, "DELETED").then().statusCode(400);
    as(adminToken)
        .contentType(ContentType.JSON)
        .body("{}")
        .put(ADMIN_USERS + "/" + anaId + "/status")
        .then()
        .statusCode(400);
    setStatus(adminToken, 999999, "BLOCKED").then().statusCode(404);
  }

  // --- The administrator's own account ---

  @Test
  void theAdministratorCannotBlockOrEditTheirOwnAccount() {
    setStatus(adminToken, adminId, "BLOCKED").then().statusCode(403);
    edit(adminToken, adminId, "Boss", "boss@example.com").then().statusCode(403);

    as(adminToken).get(ME).then().statusCode(200).body("email", equalTo(ADMIN_EMAIL));
  }
}
