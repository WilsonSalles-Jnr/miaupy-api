package com.miaupy.onboarding.domain;

public class IdentityProviderUnavailableException extends RuntimeException {
  public IdentityProviderUnavailableException(String message) {
    super(message);
  }

  public IdentityProviderUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
