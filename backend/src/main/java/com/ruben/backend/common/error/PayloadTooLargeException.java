package com.ruben.backend.common.error;

/** The request content exceeds the allowed size; answered with 413. */
public class PayloadTooLargeException extends RuntimeException {

  public PayloadTooLargeException(String message) {
    super(message);
  }
}
