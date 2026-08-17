package com.miaupy.onboarding.infrastructure;

import com.miaupy.employee.domain.EmployeeRole;
import com.miaupy.onboarding.domain.IdentityProvider;
import com.miaupy.onboarding.domain.IdentityProviderUnavailableException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class KeycloakIdentityProvider implements IdentityProvider {
  private final RestClient restClient;
  private final String realm;
  private final String clientId;
  private final String clientSecret;
  private final String consumerClientId;
  private final String businessClientId;
  private final String frontendBaseUrl;

  KeycloakIdentityProvider(
      RestClient.Builder builder,
      @Value("${miaupy.identity.base-url}") String baseUrl,
      @Value("${miaupy.identity.realm}") String realm,
      @Value("${miaupy.identity.client-id}") String clientId,
      @Value("${miaupy.identity.client-secret}") String clientSecret,
      @Value("${miaupy.identity.consumer-client-id}") String consumerClientId,
      @Value("${miaupy.identity.business-client-id:miaupy-business}") String businessClientId,
      @Value("${miaupy.identity.frontend-base-url:http://localhost:3000}") String frontendBaseUrl) {
    this.restClient = builder.baseUrl(baseUrl).build();
    this.realm = realm;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.consumerClientId = consumerClientId;
    this.businessClientId = businessClientId;
    this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
  }

  @Override
  public RegistrationResult registerConsumer(String name, String email, String password) {
    RegistrationResult result =
        registerUser(name, email, password, false, false, List.of("VERIFY_EMAIL"));
    result
        .createdSubject()
        .ifPresent(
            subject -> {
              try {
                sendVerificationEmail(adminToken(), subject, consumerClientId);
              } catch (RuntimeException exception) {
                deleteUnverifiedUser(subject);
                throw exception;
              }
            });
    return result;
  }

  @Override
  public RegistrationResult registerBusiness(String ownerName, String email, String password) {
    return registerUser(ownerName, email, password, false, false, List.of("VERIFY_EMAIL"));
  }

  @Override
  public RegistrationResult registerEmployee(
      String name, String email, String temporaryPassword, Long tenantId, EmployeeRole role) {
    RegistrationResult result =
        registerUser(name, email, temporaryPassword, true, true, List.of("UPDATE_PASSWORD"));
    result
        .createdSubject()
        .ifPresent(
            subject -> {
              try {
                grantBusinessRoleAccess(subject, tenantId, role.name());
              } catch (RuntimeException exception) {
                deleteUnverifiedUser(subject);
                throw exception;
              }
            });
    return result;
  }

  private RegistrationResult registerUser(
      String name,
      String email,
      String password,
      boolean emailVerified,
      boolean temporaryPassword,
      List<String> requiredActions) {
    String token = adminToken();
    NameParts nameParts = NameParts.from(name);
    try {
      Map<String, Object> userRepresentation = new LinkedHashMap<>();
      userRepresentation.put("username", email);
      userRepresentation.put("email", email);
      userRepresentation.put("firstName", nameParts.firstName());
      if (nameParts.lastName() != null) {
        userRepresentation.put("lastName", nameParts.lastName());
      }
      userRepresentation.put("enabled", true);
      userRepresentation.put("emailVerified", emailVerified);
      userRepresentation.put("requiredActions", requiredActions);
      userRepresentation.put(
          "credentials",
          List.of(Map.of("type", "password", "value", password, "temporary", temporaryPassword)));
      ResponseEntity<Void> response =
          restClient
              .post()
              .uri("/admin/realms/{realm}/users", realm)
              .header(HttpHeaders.AUTHORIZATION, bearer(token))
              .contentType(MediaType.APPLICATION_JSON)
              .body(userRepresentation)
              .retrieve()
              .toBodilessEntity();
      String userId = userId(response.getHeaders().getLocation());
      return new RegistrationResult(true, userId);
    } catch (HttpClientErrorException.Conflict exception) {
      return new RegistrationResult(false, null);
    } catch (RestClientException exception) {
      throw new IdentityProviderUnavailableException(
          "Identity provider did not accept the registration", exception);
    }
  }

  @Override
  public void sendBusinessVerification(String authSubject) {
    sendVerificationEmail(adminToken(), authSubject, businessClientId);
  }

  @Override
  public void deleteUnverifiedUser(String authSubject) {
    deleteUnverifiedUser(adminToken(), authSubject);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void grantBusinessAccess(String authSubject, Long tenantId) {
    grantBusinessRoleAccess(authSubject, tenantId, "OWNER");
  }

  @Override
  @SuppressWarnings("unchecked")
  public void setUserEnabled(String authSubject, boolean enabled) {
    String token = adminToken();
    try {
      Map<String, Object> user =
          restClient
              .get()
              .uri("/admin/realms/{realm}/users/{id}", realm, authSubject)
              .header(HttpHeaders.AUTHORIZATION, bearer(token))
              .retrieve()
              .body(Map.class);
      if (user == null) {
        throw new IdentityProviderUnavailableException("Identity user was not found");
      }
      Map<String, Object> updated = new LinkedHashMap<>(user);
      updated.put("enabled", enabled);
      restClient
          .put()
          .uri("/admin/realms/{realm}/users/{id}", realm, authSubject)
          .header(HttpHeaders.AUTHORIZATION, bearer(token))
          .contentType(MediaType.APPLICATION_JSON)
          .body(updated)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException exception) {
      throw new IdentityProviderUnavailableException(
          "Identity provider did not update the user status", exception);
    }
  }

  @SuppressWarnings("unchecked")
  private void grantBusinessRoleAccess(String authSubject, Long tenantId, String roleName) {
    String token = adminToken();
    try {
      Map<String, Object> user =
          restClient
              .get()
              .uri("/admin/realms/{realm}/users/{id}", realm, authSubject)
              .header(HttpHeaders.AUTHORIZATION, bearer(token))
              .retrieve()
              .body(Map.class);
      if (user == null) {
        throw new IdentityProviderUnavailableException("Identity user was not found");
      }
      Map<String, Object> updated = new LinkedHashMap<>(user);
      Map<String, Object> attributes =
          user.get("attributes") instanceof Map<?, ?> existing
              ? new LinkedHashMap<>((Map<String, Object>) existing)
              : new LinkedHashMap<>();
      attributes.put("tenant_id", List.of(tenantId.toString()));
      updated.put("attributes", attributes);
      restClient
          .put()
          .uri("/admin/realms/{realm}/users/{id}", realm, authSubject)
          .header(HttpHeaders.AUTHORIZATION, bearer(token))
          .contentType(MediaType.APPLICATION_JSON)
          .body(updated)
          .retrieve()
          .toBodilessEntity();

      Map<String, Object> role =
          restClient
              .get()
              .uri("/admin/realms/{realm}/roles/{role}", realm, roleName)
              .header(HttpHeaders.AUTHORIZATION, bearer(token))
              .retrieve()
              .body(Map.class);
      restClient
          .post()
          .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, authSubject)
          .header(HttpHeaders.AUTHORIZATION, bearer(token))
          .contentType(MediaType.APPLICATION_JSON)
          .body(new ArrayList<>(List.of(role)))
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException exception) {
      throw new IdentityProviderUnavailableException(
          "Identity provider did not complete the business access grant", exception);
    }
  }

  private String adminToken() {
    LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "client_credentials");
    form.add("client_id", clientId);
    form.add("client_secret", clientSecret);
    try {
      TokenResponse response =
          restClient
              .post()
              .uri("/realms/{realm}/protocol/openid-connect/token", realm)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(form)
              .retrieve()
              .body(TokenResponse.class);
      if (response == null || response.accessToken() == null) {
        throw new IdentityProviderUnavailableException("Identity provider returned no token");
      }
      return response.accessToken();
    } catch (RestClientException exception) {
      throw new IdentityProviderUnavailableException("Identity provider is unavailable", exception);
    }
  }

  private void sendVerificationEmail(String token, String userId, String verificationClientId) {
    String actor = verificationClientId.equals(businessClientId) ? "business" : "consumer";
    String redirectUri = frontendBaseUrl + "/api/auth/login?actor=" + actor;
    restClient
        .put()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/admin/realms/{realm}/users/{id}/execute-actions-email")
                    .queryParam("client_id", verificationClientId)
                    .queryParam("redirect_uri", redirectUri)
                    .build(realm, userId))
        .header(HttpHeaders.AUTHORIZATION, bearer(token))
        .contentType(MediaType.APPLICATION_JSON)
        .body(List.of("VERIFY_EMAIL"))
        .retrieve()
        .toBodilessEntity();
  }

  private void deleteUnverifiedUser(String token, String userId) {
    try {
      restClient
          .delete()
          .uri("/admin/realms/{realm}/users/{id}", realm, userId)
          .header(HttpHeaders.AUTHORIZATION, bearer(token))
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException ignored) {
      // The original delivery error is more useful; orphan cleanup is operationally observable in
      // Keycloak.
    }
  }

  private String userId(URI location) {
    if (location == null || location.getPath() == null) {
      throw new IdentityProviderUnavailableException("Identity provider returned no user location");
    }
    String path = location.getPath();
    return path.substring(path.lastIndexOf('/') + 1);
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }

  private record TokenResponse(
      @com.fasterxml.jackson.annotation.JsonProperty("access_token") String accessToken) {}

  private record NameParts(String firstName, String lastName) {
    private static NameParts from(String fullName) {
      String normalized = fullName.strip().replaceAll("\\s+", " ");
      int separator = normalized.indexOf(' ');
      return separator < 0
          ? new NameParts(normalized, null)
          : new NameParts(normalized.substring(0, separator), normalized.substring(separator + 1));
    }
  }
}
