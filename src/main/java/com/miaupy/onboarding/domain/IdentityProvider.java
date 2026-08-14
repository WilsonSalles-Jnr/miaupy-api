package com.miaupy.onboarding.domain;

import java.util.Optional;

public interface IdentityProvider {

  RegistrationResult registerConsumer(String name, String email, String password);

  RegistrationResult registerBusiness(String ownerName, String email, String password);

  void grantBusinessAccess(String authSubject, Long tenantId);

  void sendBusinessVerification(String authSubject);

  void deleteUnverifiedUser(String authSubject);

  record RegistrationResult(boolean created, String authSubject) {
    public Optional<String> createdSubject() {
      return created ? Optional.ofNullable(authSubject) : Optional.empty();
    }
  }
}
