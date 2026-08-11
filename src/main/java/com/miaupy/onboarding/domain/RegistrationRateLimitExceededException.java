package com.miaupy.onboarding.domain;

public class RegistrationRateLimitExceededException extends RuntimeException {
  public RegistrationRateLimitExceededException() {
    super("Too many registration attempts. Try again later");
  }
}
