package com.ruben.backend.image.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.common.security.AdminProperties;
import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.image.repository.ImageRepository;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AdminAccountInitializer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.UUID;
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
class ImageApiE2ETest {

  private static final String IMAGES = "/api/v1/images";
  private static final String ADMIN_PASSWORD = "Admin-test-1";

  @LocalServerPort private int port;

  @Autowired private ImageRepository imageRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private AdminAccountInitializer adminAccountInitializer;
  @Autowired private AdminProperties adminProperties;

  private String token;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "https://localhost";
    RestAssured.port = port;
    RestAssured.useRelaxedHTTPSValidation();

    // Users first: an avatar is an image that a user points at.
    userRepository.deleteAll();
    imageRepository.deleteAll();
    adminAccountInitializer.run(new DefaultApplicationArguments());
    token =
        given()
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

  @AfterEach
  void tearDown() {
    RestAssured.reset();
  }

  private Response upload(byte[] data, String contentType) {
    return given()
        .cookie("AuthToken", token)
        .multiPart("file", "picture", data, contentType)
        .post(IMAGES);
  }

  @Test
  void aLoggedInUserUploadsAnImageAndGets201WithItsLocation() {
    byte[] data = ImageTestData.png(2048);

    upload(data, "image/png")
        .then()
        .statusCode(201)
        .header("Location", endsWith("/api/v1/images/" + imageRepository.findAll().get(0).getId()))
        .body("id", notNullValue())
        .body("contentType", equalTo("image/png"))
        .body("size", equalTo(2048));
  }

  @Test
  void theUploadedBytesAreServedToAnonymousUsersWithTypeAndCacheHeaders() {
    byte[] data = ImageTestData.jpeg(3000);
    String location =
        upload(data, "image/jpeg").then().statusCode(201).extract().header("Location");

    Response response = given().get(location);

    response
        .then()
        .statusCode(200)
        .contentType("image/jpeg")
        .header("Cache-Control", "max-age=31536000, public, immutable");
    assertThat(response.asByteArray()).isEqualTo(data);
  }

  @Test
  void aWebpImageIsAccepted() {
    upload(ImageTestData.webp(500), "image/webp").then().statusCode(201);
  }

  @Test
  void aTypeThatIsNotAllowedReturns400() {
    upload(ImageTestData.text(), "text/plain")
        .then()
        .statusCode(400)
        .body("status", equalTo(400))
        .body("message", equalTo("Only JPEG, PNG and WebP images are accepted"));
  }

  @Test
  void aTextFileDeclaredAsPngReturns400() {
    upload(ImageTestData.text(), "image/png").then().statusCode(400);
    assertThat(imageRepository.count()).isZero();
  }

  @Test
  void anEmptyFileReturns400() {
    upload(new byte[0], "image/png").then().statusCode(400);
  }

  @Test
  void aRequestWithoutTheFilePartReturns400() {
    given()
        .cookie("AuthToken", token)
        .multiPart("other", "value")
        .post(IMAGES)
        .then()
        .statusCode(400)
        .body("message", equalTo("Missing part 'file'"));
  }

  @Test
  void anImageLargerThanTheLimitReturns413() {
    upload(ImageTestData.png(6 * 1024 * 1024), "image/png")
        .then()
        .statusCode(413)
        .body("status", equalTo(413));
    assertThat(imageRepository.count()).isZero();
  }

  @Test
  void anAnonymousUploadReturns401() {
    given()
        .multiPart("file", "picture", ImageTestData.png(100), "image/png")
        .post(IMAGES)
        .then()
        .statusCode(401)
        .body("status", equalTo(401));
    assertThat(imageRepository.count()).isZero();
  }

  @Test
  void anUnknownImageReturns404() {
    given()
        .get(IMAGES + "/" + UUID.randomUUID())
        .then()
        .statusCode(404)
        .body("message", equalTo("Image not found"));
  }

  @Test
  void anIdThatIsNotAUuidReturns400() {
    given().get(IMAGES + "/abc").then().statusCode(400);
  }
}
