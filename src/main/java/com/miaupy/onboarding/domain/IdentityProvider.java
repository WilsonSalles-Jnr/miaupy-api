package com.miaupy.onboarding.domain;

import com.miaupy.employee.domain.EmployeeRole;
import java.util.Optional;

public interface IdentityProvider {

  RegistrationResult registerConsumer(String name, String email, String password);

  RegistrationResult registerBusiness(String ownerName, String email, String password);

  RegistrationResult registerEmployee(
      String name, String email, String temporaryPassword, Long tenantId, EmployeeRole role);

  void grantBusinessAccess(String authSubject, Long tenantId);

  void sendBusinessVerification(String authSubject);

  void deleteUnverifiedUser(String authSubject);

  void setUserEnabled(String authSubject, boolean enabled);

  record RegistrationResult(boolean created, String authSubject) {
    public Optional<String> createdSubject() {
      return created ? Optional.ofNullable(authSubject) : Optional.empty();
    }
  }
}
