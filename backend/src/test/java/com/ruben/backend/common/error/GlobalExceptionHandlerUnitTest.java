package com.ruben.backend.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@Tag("B2")
class GlobalExceptionHandlerUnitTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
  private final MockHttpServletRequest request =
      new MockHttpServletRequest("POST", "/api/v1/auth/login");

  @SuppressWarnings("unused")
  private void bodyHandler(Object body) {}

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
}
