package com.ruben.backend.common.config;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.common.security.AdminProperties;
import com.ruben.backend.user.service.AdminAccountInitializer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

@Tag("docker")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class SpaForwardingApiE2ETest {

  private static final String ADMIN_PASSWORD = "Admin-test-1";
  private static final String SPA_MARKER = "TripMate test SPA";

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
  void theRootReturnsIndexHtml() {
    given()
        .get("/")
        .then()
        .statusCode(200)
        .contentType(ContentType.HTML)
        .body(containsString(SPA_MARKER));
  }

  @Test
  void aClientRouteReturnsIndexHtml() {
    given()
        .get("/trips")
        .then()
        .statusCode(200)
        .contentType(ContentType.HTML)
        .body(containsString(SPA_MARKER));
  }

  @Test
  void aNestedClientRouteReturnsIndexHtml() {
    given()
        .get("/trips/12/edit")
        .then()
        .statusCode(200)
        .contentType(ContentType.HTML)
        .body(containsString(SPA_MARKER));
  }

  @Test
  void anUnknownApiUrlStillReturnsTheJsonError() {
    given()
        .cookie("AuthToken", adminToken())
        .get("/api/v1/unknown")
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .body("status", equalTo(404))
        .body("path", equalTo("/api/v1/unknown"))
        .body("message", not(containsString(SPA_MARKER)));
  }

  @Test
  void aMissingFileReturns404() {
    given().get("/missing.png").then().statusCode(404);
  }
}
