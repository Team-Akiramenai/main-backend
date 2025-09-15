package com.akiramenai.backend.model;

public class VttValidationResult {
  private final boolean valid;
  private final String message;

  public VttValidationResult(boolean valid, String message) {
    this.valid = valid;
    this.message = message;
  }

  public boolean isValid() {
    return valid;
  }

  public String getMessage() {
    return message;
  }
}
