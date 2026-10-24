package com.ruben.backend.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@Tag("B2")
class GlobalExceptionHandlerUnitTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
  private final MockHttpServletRequest request =
      new MockHttpServletRequest("POST", "/api/v1/auth/login");

  @SuppressWarnings("unused")
  private void bodyHandler(Object body) {
    // Never called: it only gives MethodParameter a real method parameter to point at.
  }

  @Test
  void badCredentialsAreAnswered401WithAGenericMessage() {
    ResponseEntity<ErrorResponse> response = handler.handleBadCredentials(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody().status()).isEqualTo(401);
    assertThat(response.getBody().error()).isEqualTo("Unauthorized");
    assertThat(response.getBody().message()).isEqualTo("Invalid email or password");
    assertThat(response.getBody().path()).isEqualTo("/api/v1/auth/login");
    assertThat(response.getBody().timestamp()).isNotNull();
  }

  @Test
  void aBlockedAccountIsAnswered403() {
    ResponseEntity<ErrorResponse> response = handler.handleLocked(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody().error()).isEqualTo("Forbidden");
    assertThat(response.getBody().message()).isEqualTo("Account is blocked");
  }

  @Test
  void anInvalidBodyIsAnswered400ListingTheFieldsSortedByName() throws Exception {
    BeanPropertyBindingResult bindingResult =
        new BeanPropertyBindingResult(new Object(), "loginRequest");
    bindingResult.addError(new FieldError("loginRequest", "password", "must not be blank"));
    bindingResult.addError(
        new FieldError("loginRequest", "email", "must be a well-formed email address"));
    MethodParameter parameter =
        new MethodParameter(
            GlobalExceptionHandlerUnitTest.class.getDeclaredMethod("bodyHandler", Object.class), 0);

    ResponseEntity<ErrorResponse> response =
        handler.handleInvalidBody(
            new MethodArgumentNotValidException(parameter, bindingResult), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message())
        .isEqualTo("email: must be a well-formed email address; password: must not be blank");
  }

  @Test
  void aMissingResourceIsAnswered404WithItsMessage() {
    ResponseEntity<ErrorResponse> response =
        handler.handleNotFound(new ResourceNotFoundException("Trip not found"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody().error()).isEqualTo("Not Found");
    assertThat(response.getBody().message()).isEqualTo("Trip not found");
    assertThat(response.getBody().path()).isEqualTo("/api/v1/auth/login");
  }

  @Test
  void anUnknownUrlIsAnswered404() {
    ResponseEntity<ErrorResponse> response = handler.handleNoResource(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody().message()).isEqualTo("Resource not found");
  }

  @Test
  void aConflictIsAnswered409WithItsMessage() {
    ResponseEntity<ErrorResponse> response =
        handler.handleConflict(new ConflictException("Email already registered"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody().error()).isEqualTo("Conflict");
    assertThat(response.getBody().message()).isEqualTo("Email already registered");
  }

  @Test
  void aForbiddenActionIsAnswered403WithItsMessage() {
    ResponseEntity<ErrorResponse> response =
        handler.handleForbidden(new ForbiddenException("Not your profile"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody().error()).isEqualTo("Forbidden");
    assertThat(response.getBody().message()).isEqualTo("Not your profile");
  }

  @Test
  void accessDeniedByMethodSecurityIsAnswered403() {
    ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody().message()).isEqualTo("Access is denied");
  }

  @Test
  void otherAuthenticationFailuresAreAnswered401() {
    ResponseEntity<ErrorResponse> response = handler.handleAuthentication(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody().message()).isEqualTo("Authentication is required");
  }

  @Test
  void malformedJsonIsAnswered400() {
    ResponseEntity<ErrorResponse> response = handler.handleMalformedJson(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).isEqualTo("Malformed JSON request");
  }

  @Test
  void aParameterOfTheWrongTypeIsAnswered400NamingTheParameter() {
    MethodArgumentTypeMismatchException exception =
        new MethodArgumentTypeMismatchException("abc", Long.class, "id", null, null);

    ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).isEqualTo("Invalid value for parameter 'id'");
  }

  @Test
  void sortingByAnUnknownPropertyIsAnswered400NamingIt() {
    PropertyReferenceException exception =
        new PropertyReferenceException("bogus", TypeInformation.of(Object.class), List.of());

    ResponseEntity<ErrorResponse> response = handler.handleUnknownSortProperty(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).isEqualTo("Cannot sort by 'bogus'");
  }

  @Test
  void aMissingParameterIsAnswered400NamingTheParameter() {
    ResponseEntity<ErrorResponse> response =
        handler.handleMissingParameter(
            new MissingServletRequestParameterException("page", "int"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).isEqualTo("Missing parameter 'page'");
  }

  @Test
  void anUnsupportedMethodIsAnswered405WithTheAllowHeader() {
    ResponseEntity<ErrorResponse> response =
        handler.handleMethodNotSupported(
            new HttpRequestMethodNotSupportedException("DELETE", List.of("GET", "POST")), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(response.getHeaders().getAllow())
        .containsExactlyInAnyOrder(HttpMethod.GET, HttpMethod.POST);
    assertThat(response.getBody().message()).isEqualTo("Method DELETE is not supported");
  }

  @Test
  void anUnsupportedMethodWithoutKnownAlternativesHasNoAllowHeader() {
    ResponseEntity<ErrorResponse> response =
        handler.handleMethodNotSupported(
            new HttpRequestMethodNotSupportedException("DELETE"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(response.getHeaders().getAllow()).isEmpty();
  }

  @Test
  void anUnexpectedErrorIsAnswered500WithAGenericMessageAndNoCause() {
    ResponseEntity<ErrorResponse> response =
        handler.handleUnexpected(
            new IllegalStateException("database password is hunter2"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
    assertThat(response.getBody().message()).isEqualTo("Unexpected error");
    assertThat(response.getBody().toString()).doesNotContain("hunter2");
  }

  @Test
  void aBadRequestIsAnswered400WithItsMessage() {
    ResponseEntity<ErrorResponse> response =
        handler.handleBadRequest(new BadRequestException("Not acceptable"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).isEqualTo("Not acceptable");
  }

  @Test
  void aTooLargePayloadIsAnswered413WithItsMessage() {
    ResponseEntity<ErrorResponse> response =
        handler.handleTooLarge(new PayloadTooLargeException("Too big"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
    assertThat(response.getBody().error()).isEqualTo("Content Too Large");
    assertThat(response.getBody().message()).isEqualTo("Too big");
  }

  @Test
  void anUploadOverTheMultipartLimitIsAnswered413() {
    ResponseEntity<ErrorResponse> response = handler.handleUploadLimit(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
    assertThat(response.getBody().message()).isEqualTo("The uploaded file is too large");
  }

  @Test
  void aMissingMultipartPartIsAnswered400NamingIt() {
    ResponseEntity<ErrorResponse> response =
        handler.handleMissingPart(new MissingServletRequestPartException("file"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).isEqualTo("Missing part 'file'");
  }
}
