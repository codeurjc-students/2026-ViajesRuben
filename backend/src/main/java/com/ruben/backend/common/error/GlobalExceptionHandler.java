package com.ruben.backend.common.error;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BadCredentialsException.class)
  ResponseEntity<ErrorResponse> handleBadCredentials(HttpServletRequest request) {
    return build(HttpStatus.UNAUTHORIZED, "Invalid email or password", request);
  }

  @ExceptionHandler(LockedException.class)
  ResponseEntity<ErrorResponse> handleLocked(HttpServletRequest request) {
    return build(HttpStatus.FORBIDDEN, "Account is blocked", request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErrorResponse> handleInvalidBody(
      MethodArgumentNotValidException e, HttpServletRequest request) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .sorted(Comparator.comparing(error -> error.getField()))
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
    return build(HttpStatus.BAD_REQUEST, message, request);
  }

  private static ResponseEntity<ErrorResponse> build(
      HttpStatus status, String message, HttpServletRequest request) {
    return ResponseEntity.status(status)
        .body(ErrorResponse.of(status, message, request.getRequestURI()));
  }
}
