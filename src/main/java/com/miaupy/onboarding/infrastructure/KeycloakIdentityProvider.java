package com.miaupy.onboarding.infrastructure;

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

  KeycloakIdentityProvider(
      RestClient.Builder builder,
      @Value("${miaupy.identity.base-url}") String baseUrl,
      @Value("${miaupy.identity.realm}") String realm,
      @Value("${miaupy.identity.client-id}") String clientId,
      @Value("${miaupy.identity.client-secret}") String clientSecret,
      @Value("${miaupy.identity.consumer-client-id}") String consumerClientId) {
    this.restClient = builder.baseUrl(baseUrl).build();
    this.realm = realm;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.consumerClientId = consumerClientId;
  }

  @Override
  public RegistrationResult registerConsumer(String name, String email, String password) {
    String token = adminToken();
    try {
      ResponseEntity<Void> response =
          restClient
              .post()
              .uri("/admin/realms/{realm}/users", realm)
              .header(HttpHeaders.AUTHORIZATION, bearer(token))
              .contentType(MediaType.APPLICATION_JSON)
              .body(
                  Map.of(
                      "username",
                      email,
                      "email",
                      email,
                      "firstName",
                      name,
                      "enabled",
                      true,
                      "emailVerified",
                      false,
                      "requiredActions",
                      List.of("VERIFY_EMAIL"),
                      "credentials",
                      List.of(Map.of("type", "password", "value", password, "temporary", false))))
              .retrieve()
              .toBodilessEntity();
      String userId = userId(response.getHeaders().getLocation());
      try {
        sendVerificationEmail(token, userId);
      } catch (RuntimeException exception) {
        deleteUnverifiedUser(token, userId);
        throw exception;
      }
      return new RegistrationResult(true);
    } catch (HttpClientErrorException.Conflict exception) {
      return new RegistrationResult(false);
    } catch (RestClientException exception) {
      throw new IdentityProviderUnavailableException(
          "Identity provider did not accept the registration", exception);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void grantBusinessAccess(String authSubject, Long tenantId) {
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

      Map<String, Object> ownerRole =
          restClient
              .get()
              .uri("/admin/realms/{realm}/roles/OWNER", realm)
              .header(HttpHeaders.AUTHORIZATION, bearer(token))
              .retrieve()
              .body(Map.class);
      restClient
          .post()
          .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, authSubject)
          .header(HttpHeaders.AUTHORIZATION, bearer(token))
          .contentType(MediaType.APPLICATION_JSON)
          .body(new ArrayList<>(List.of(ownerRole)))
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

  private void sendVerificationEmail(String token, String userId) {
    restClient
        .put()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/admin/realms/{realm}/users/{id}/execute-actions-email")
                    .queryParam("client_id", consumerClientId)
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
}
