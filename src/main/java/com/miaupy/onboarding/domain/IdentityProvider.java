package com.miaupy.onboarding.domain;

public interface IdentityProvider {

  RegistrationResult registerConsumer(String name, String email, String password);

  void grantBusinessAccess(String authSubject, Long tenantId);

  record RegistrationResult(boolean created) {}
}
