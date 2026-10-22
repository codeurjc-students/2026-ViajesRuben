package com.ruben.backend.common.error;

/** The request is well formed but its content is not acceptable; answered with 400. */
public class BadRequestException extends RuntimeException {

  public BadRequestException(String message) {
    super(message);
  }
}
