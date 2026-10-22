package com.ruben.backend.common.error;

/** The request clashes with the current state of a resource; answered with 409. */
public class ConflictException extends RuntimeException {

  public ConflictException(String message) {
    super(message);
  }
}
