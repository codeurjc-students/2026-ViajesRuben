package com.ruben.backend.common.error;

/** The user is logged in but may not do this (not the owner, or a role restriction); 403. */
public class ForbiddenException extends RuntimeException {

  public ForbiddenException(String message) {
    super(message);
  }
}
