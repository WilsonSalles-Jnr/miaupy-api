package com.miaupy.onboarding.application;

public interface RegistrationRateLimiter {
  void check(String remoteAddress, String normalizedEmail);
}
