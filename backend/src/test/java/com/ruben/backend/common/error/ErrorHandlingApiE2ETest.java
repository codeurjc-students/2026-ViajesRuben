package com.ruben.backend.common.error;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.common.security.AdminProperties;
import com.ruben.backend.user.service.AdminAccountInitializer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ErrorHandlingApiE2ETest {

  private static final String ADMIN_PASSWORD = "Admin-test-1";

  @LocalServerPort private int port;

  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private AdminAccountInitializer adminAccountInitializer;
  @Autowired private AdminProperties adminProperties;

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

  private String adminToken() {
    return given()
        .contentType(ContentType.JSON)
        .body(
            "{\"email\":\"%s\",\"password\":\"%s\"}"
                .formatted(adminProperties.email(), ADMIN_PASSWORD))
        .post("/api/v1/auth/login")
        .then()
        .statusCode(200)
        .extract()
        .cookie("AuthToken");
  }

  @Test
  void anUnknownTripReturns404WithTheErrorBody() {
    given()
        .cookie("AuthToken", adminToken())
        .get("/api/v1/trips/999999")
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .body("status", equalTo(404))
        .body("error", equalTo("Not Found"))
        .body("message", equalTo("Trip not found"))
        .body("path", equalTo("/api/v1/trips/999999"))
        .body("timestamp", notNullValue());
  }

  @Test
  void anInvalidBodyReturns400ListingTheFields() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"email\":\"not-an-email\",\"password\":\"\"}")
        .post("/api/v1/auth/login")
        .then()
        .statusCode(400)
        .body("status", equalTo(400))
        .body("error", equalTo("Bad Request"))
        .body(
            "message",
            equalTo("email: must be a well-formed email address; password: must not be blank"))
        .body("path", equalTo("/api/v1/auth/login"));
  }

  @Test
  void malformedJsonReturns400() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"email\": ")
        .post("/api/v1/auth/login")
        .then()
        .statusCode(400)
        .body("message", equalTo("Malformed JSON request"));
  }

  @Test
  void aNonNumericTripIdReturns400() {
    given()
        .cookie("AuthToken", adminToken())
        .get("/api/v1/trips/abc")
        .then()
        .statusCode(400)
        .body("message", equalTo("Invalid value for parameter 'id'"));
  }

  @Test
  void anUnsupportedMethodReturns405() {
    given()
        .cookie("AuthToken", adminToken())
        .delete("/api/v1/trips")
        .then()
        .statusCode(405)
        .header("Allow", containsString("GET"))
        .body("status", equalTo(405))
        .body("error", equalTo("Method Not Allowed"));
  }

  @Test
  void anUnknownApiUrlReturns404WithTheErrorBody() {
    given()
        .cookie("AuthToken", adminToken())
        .get("/api/v1/does-not-exist")
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .body("status", equalTo(404))
        .body("message", not(containsString("Exception")));
  }
}
