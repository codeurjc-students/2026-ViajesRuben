package com.ruben.backend.user.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.user.service.AdminAccountInitializer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

@Tag("B3")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class UserProfileApiE2ETest {

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
  private String bobToken;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "https://localhost";
    RestAssured.port = port;
    RestAssured.useRelaxedHTTPSValidation();

    databaseCleaner.clear();
    adminAccountInitializer.run(new DefaultApplicationArguments());
    anaId = register("Ana", "ana@example.com");
    anaToken =
        login("ana@example.com", PASSWORD).then().statusCode(200).extract().cookie("AuthToken");
    bobId = register("Bob", "bob@example.com");
    bobToken =
        login("bob@example.com", PASSWORD).then().statusCode(200).extract().cookie("AuthToken");
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

  private static RequestSpecification as(String token) {
    return given().cookie("AuthToken", token);
  }

  private static Response updateProfile(String token, long id, String name, String email) {
    return as(token)
        .contentType(ContentType.JSON)
        .body("{\"name\":\"%s\",\"email\":\"%s\"}".formatted(name, email))
        .put(USERS + "/" + id);
  }

  private static Response changePassword(String token, long id, String current, String next) {
    return as(token)
        .contentType(ContentType.JSON)
        .body("{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}".formatted(current, next))
        .put(USERS + "/" + id + "/password");
  }

  private static Response uploadAvatar(String token, long id, byte[] data, String type) {
    return as(token).multiPart("file", "picture", data, type).put(USERS + "/" + id + "/avatar");
  }

  // --- Not logged in ---

  @Test
  void everyProfileOperationRequiresLogin() {
    String profile = USERS + "/" + anaId;
    given().get(profile).then().statusCode(401).body("status", equalTo(401));
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Ana\",\"email\":\"ana@example.com\"}")
        .put(profile)
        .then()
        .statusCode(401);
    given()
        .contentType(ContentType.JSON)
        .body("{\"currentPassword\":\"secret-pass\",\"newPassword\":\"other-pass\"}")
        .put(profile + "/password")
        .then()
        .statusCode(401);
    given()
        .multiPart("file", "picture", ImageTestData.png(100), "image/png")
        .put(profile + "/avatar")
        .then()
        .statusCode(401);
    given().get(profile + "/avatar").then().statusCode(401);
    given().delete(profile + "/avatar").then().statusCode(401);
  }

  // --- Read ---

  @Test
  void aUserReadsTheirOwnProfileWithoutThePassword() {
    as(anaToken)
        .get(USERS + "/" + anaId)
        .then()
        .statusCode(200)
        .body("id", equalTo((int) anaId))
        .body("name", equalTo("Ana"))
        .body("email", equalTo("ana@example.com"))
        .body("role", equalTo("USER"))
        .body("avatarId", nullValue())
        .body("registrationDate", equalTo(LocalDate.now().toString()))
        .body("password", nullValue());
  }

  @Test
  void anotherUsersProfileIsForbidden() {
    as(anaToken)
        .get(USERS + "/" + bobId)
        .then()
        .statusCode(403)
        .body("message", equalTo("Only the user can access their own profile"));
  }

  @Test
  void aMissingUserIsNotFound() {
    as(anaToken)
        .get(USERS + "/999999")
        .then()
        .statusCode(404)
        .body("message", equalTo("User not found"));
  }

  // --- Edit name and email ---

  @Test
  void anEmailChangeKeepsTheSessionAndOnlyTheNewEmailLogsIn() {
    updateProfile(anaToken, anaId, "Ana Ruiz", "Ana.R@Example.COM")
        .then()
        .statusCode(200)
        .body("name", equalTo("Ana Ruiz"))
        .body("email", equalTo("ana.r@example.com"))
        .body("password", nullValue());

    as(anaToken)
        .get(ME)
        .then()
        .statusCode(200)
        .body("name", equalTo("Ana Ruiz"))
        .body("email", equalTo("ana.r@example.com"));
    login("ana.r@example.com", PASSWORD).then().statusCode(200);
    login("ana@example.com", PASSWORD).then().statusCode(401);
  }

  @Test
  void theEmailOfAnotherAccountIsAConflict() {
    updateProfile(anaToken, anaId, "Ana", "BOB@example.com")
        .then()
        .statusCode(409)
        .body("message", equalTo("Email is already registered"));
  }

  @Test
  void theAdministratorEmailIsAConflict() {
    updateProfile(anaToken, anaId, "Ana", ADMIN_EMAIL).then().statusCode(409);
  }

  @Test
  void anInvalidProfileIsABadRequest() {
    updateProfile(anaToken, anaId, " ", "ana@example.com").then().statusCode(400);
    updateProfile(anaToken, anaId, "Ana", "not-an-email").then().statusCode(400);
    as(anaToken)
        .contentType(ContentType.JSON)
        .body("{\"name\":")
        .put(USERS + "/" + anaId)
        .then()
        .statusCode(400)
        .body("message", equalTo("Malformed JSON request"));
  }

  @Test
  void editingAnotherUsersProfileIsForbidden() {
    updateProfile(anaToken, bobId, "Hacked", "hacked@example.com").then().statusCode(403);
    as(bobToken).get(ME).then().body("email", equalTo("bob@example.com"));
  }

  @Test
  void theAdministratorCannotEditTheirProfileOrPassword() {
    String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD).then().extract().cookie("AuthToken");
    long adminId = as(adminToken).get(ME).then().extract().jsonPath().getLong("id");

    updateProfile(adminToken, adminId, "Boss", "boss@example.com")
        .then()
        .statusCode(403)
        .body("message", equalTo("The administrator account is managed by the configuration"));
    changePassword(adminToken, adminId, ADMIN_PASSWORD, "brand-new-pass").then().statusCode(403);
    login(ADMIN_EMAIL, ADMIN_PASSWORD).then().statusCode(200);
  }

  // --- Password ---

  @Test
  void aChangedPasswordReplacesTheOldOne() {
    changePassword(anaToken, anaId, PASSWORD, "brand-new-pass").then().statusCode(204);

    login("ana@example.com", "brand-new-pass").then().statusCode(200);
    login("ana@example.com", PASSWORD).then().statusCode(401);
  }

  @Test
  void aWrongCurrentPasswordIsABadRequest() {
    changePassword(anaToken, anaId, "wrong-pass", "brand-new-pass")
        .then()
        .statusCode(400)
        .body("message", equalTo("Current password is incorrect"));
    login("ana@example.com", PASSWORD).then().statusCode(200);
  }

  @Test
  void aNewPasswordOutsideEightToSeventyTwoCharactersIsABadRequest() {
    changePassword(anaToken, anaId, PASSWORD, "short").then().statusCode(400);
    changePassword(anaToken, anaId, PASSWORD, "x".repeat(73)).then().statusCode(400);
  }

  @Test
  void changingAnotherUsersPasswordIsForbidden() {
    changePassword(anaToken, bobId, PASSWORD, "brand-new-pass").then().statusCode(403);
    login("bob@example.com", PASSWORD).then().statusCode(200);
  }

  // --- Avatar ---

  @Test
  void theFirstAvatarIsCreatedAndASecondOneReplacesIt() {
    String first =
        uploadAvatar(anaToken, anaId, ImageTestData.png(100), "image/png")
            .then()
            .statusCode(201)
            .header("Location", endsWith("/api/v1/users/" + anaId + "/avatar"))
            .body("contentType", equalTo("image/png"))
            .extract()
            .jsonPath()
            .getString("id");

    String second =
        uploadAvatar(anaToken, anaId, ImageTestData.jpeg(200), "image/jpeg")
            .then()
            .statusCode(200)
            .header("Location", nullValue())
            .body("contentType", equalTo("image/jpeg"))
            .extract()
            .jsonPath()
            .getString("id");

    as(anaToken).get(ME).then().body("avatarId", equalTo(second));
    given().get("/api/v1/images/" + first).then().statusCode(404);
    given().get("/api/v1/images/" + second).then().statusCode(200);
  }

  @Test
  void anyLoggedInUserReadsAnAvatarWithoutCaching() {
    byte[] data = ImageTestData.png(150);
    uploadAvatar(anaToken, anaId, data, "image/png").then().statusCode(201);

    byte[] body =
        as(bobToken)
            .get(USERS + "/" + anaId + "/avatar")
            .then()
            .statusCode(200)
            .contentType("image/png")
            .header("Cache-Control", "no-cache")
            .extract()
            .asByteArray();
    assertThat(body).isEqualTo(data);
  }

  @Test
  void anAvatarThatIsNotAnImageIsABadRequest() {
    uploadAvatar(anaToken, anaId, ImageTestData.text(), "image/png")
        .then()
        .statusCode(400)
        .body("message", equalTo("Only JPEG, PNG and WebP images are accepted"));
    as(anaToken).get(ME).then().body("avatarId", nullValue());
  }

  @Test
  void anAvatarLargerThanTheLimitIsTooLarge() {
    uploadAvatar(anaToken, anaId, ImageTestData.png(6 * 1024 * 1024), "image/png")
        .then()
        .statusCode(413);
  }

  @Test
  void anAvatarUploadWithoutTheFilePartIsABadRequest() {
    as(anaToken)
        .multiPart("other", "value")
        .put(USERS + "/" + anaId + "/avatar")
        .then()
        .statusCode(400)
        .body("message", equalTo("Missing part 'file'"));
  }

  @Test
  void changingOrDeletingAnotherUsersAvatarIsForbidden() {
    uploadAvatar(bobToken, bobId, ImageTestData.png(100), "image/png").then().statusCode(201);

    uploadAvatar(anaToken, bobId, ImageTestData.png(100), "image/png").then().statusCode(403);
    as(anaToken).delete(USERS + "/" + bobId + "/avatar").then().statusCode(403);
    as(bobToken).get(USERS + "/" + bobId + "/avatar").then().statusCode(200);
  }

  @Test
  void theAdministratorCanChangeTheirAvatar() {
    String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD).then().extract().cookie("AuthToken");
    long adminId = as(adminToken).get(ME).then().extract().jsonPath().getLong("id");

    uploadAvatar(adminToken, adminId, ImageTestData.png(100), "image/png")
        .then()
        .statusCode(201)
        .body("id", notNullValue());
  }

  @Test
  void aDeletedAvatarIsGoneAndDeletingAgainStillAnswers204() {
    uploadAvatar(anaToken, anaId, ImageTestData.png(100), "image/png").then().statusCode(201);

    as(anaToken).delete(USERS + "/" + anaId + "/avatar").then().statusCode(204);

    as(anaToken)
        .get(USERS + "/" + anaId + "/avatar")
        .then()
        .statusCode(404)
        .body("message", equalTo("The user has no avatar"));
    as(anaToken).get(ME).then().body("avatarId", nullValue());
    as(anaToken).delete(USERS + "/" + anaId + "/avatar").then().statusCode(204);
  }

  @Test
  void theAvatarOfAMissingUserIsNotFound() {
    as(anaToken).get(USERS + "/999999/avatar").then().statusCode(404);
  }
}
