package com.ruben.backend.common.error;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ResourceNotFoundException.class)
  ResponseEntity<ErrorResponse> handleNotFound(
      ResourceNotFoundException e, HttpServletRequest request) {
    return build(HttpStatus.NOT_FOUND, e.getMessage(), request);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ErrorResponse> handleNoResource(HttpServletRequest request) {
    return build(HttpStatus.NOT_FOUND, "Resource not found", request);
  }

  @ExceptionHandler(BadRequestException.class)
  ResponseEntity<ErrorResponse> handleBadRequest(
      BadRequestException e, HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, e.getMessage(), request);
  }

  @ExceptionHandler(PayloadTooLargeException.class)
  ResponseEntity<ErrorResponse> handleTooLarge(
      PayloadTooLargeException e, HttpServletRequest request) {
    return build(HttpStatus.CONTENT_TOO_LARGE, e.getMessage(), request);
  }

  /** The upload was rejected by the multipart limits before reaching a controller. */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<ErrorResponse> handleUploadLimit(HttpServletRequest request) {
    return build(HttpStatus.CONTENT_TOO_LARGE, "The uploaded file is too large", request);
  }

  @ExceptionHandler(MissingServletRequestPartException.class)
  ResponseEntity<ErrorResponse> handleMissingPart(
      MissingServletRequestPartException e, HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, "Missing part '" + e.getRequestPartName() + "'", request);
  }

  @ExceptionHandler(ConflictException.class)
  ResponseEntity<ErrorResponse> handleConflict(ConflictException e, HttpServletRequest request) {
    return build(HttpStatus.CONFLICT, e.getMessage(), request);
  }

  @ExceptionHandler(BadCredentialsException.class)
  ResponseEntity<ErrorResponse> handleBadCredentials(HttpServletRequest request) {
    return build(HttpStatus.UNAUTHORIZED, "Invalid email or password", request);
  }

  @ExceptionHandler(LockedException.class)
  ResponseEntity<ErrorResponse> handleLocked(HttpServletRequest request) {
    return build(HttpStatus.FORBIDDEN, "Account is blocked", request);
  }

  /** Raised by method security inside controllers, where the filter-level handlers do not run. */
  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ErrorResponse> handleAccessDenied(HttpServletRequest request) {
    return build(HttpStatus.FORBIDDEN, "Access is denied", request);
  }

  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<ErrorResponse> handleAuthentication(HttpServletRequest request) {
    return build(HttpStatus.UNAUTHORIZED, "Authentication is required", request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErrorResponse> handleInvalidBody(
      MethodArgumentNotValidException e, HttpServletRequest request) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .sorted(Comparator.comparing(FieldError::getField))
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
    return build(HttpStatus.BAD_REQUEST, message, request);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ErrorResponse> handleMalformedJson(HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, "Malformed JSON request", request);
  }

  /** An unknown property in `?sort=`. */
  @ExceptionHandler(PropertyReferenceException.class)
  ResponseEntity<ErrorResponse> handleUnknownSortProperty(
      PropertyReferenceException e, HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, "Cannot sort by '" + e.getPropertyName() + "'", request);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<ErrorResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException e, HttpServletRequest request) {
    return build(
        HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + e.getName() + "'", request);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  ResponseEntity<ErrorResponse> handleMissingParameter(
      MissingServletRequestParameterException e, HttpServletRequest request) {
    return build(
        HttpStatus.BAD_REQUEST, "Missing parameter '" + e.getParameterName() + "'", request);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ErrorResponse> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
    HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
    ResponseEntity.BodyBuilder response = ResponseEntity.status(status);
    Set<HttpMethod> supportedMethods = e.getSupportedHttpMethods();
    if (supportedMethods != null) {
      HttpHeaders headers = new HttpHeaders();
      headers.setAllow(supportedMethods);
      response.headers(headers);
    }
    return response.body(
        ErrorResponse.of(
            status, "Method " + e.getMethod() + " is not supported", request.getRequestURI()));
  }

  /** Last resort: the cause is only logged, never sent to the client. */
  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
    log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), e);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request);
  }

  private static ResponseEntity<ErrorResponse> build(
      HttpStatus status, String message, HttpServletRequest request) {
    return ResponseEntity.status(status)
        .body(ErrorResponse.of(status, message, request.getRequestURI()));
  }
}
