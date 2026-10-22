package com.ruben.backend.common.error;

/** The requested resource does not exist; answered with 404. */
public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String message) {
    super(message);
  }
}
