package com.miaupy.shared.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miaupy.shared.exception.TenantAccessDeniedException;
import com.miaupy.shared.security.SecurityContextActorContext;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class JwtTenantContextTest {

  private final JwtTenantContext tenantContext =
      new JwtTenantContext(new SecurityContextActorContext());

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void readsTenantOnlyFromAuthenticatedB2bJwt() {
    authenticate(Map.of("sub", "user-1", "actor_type", "B2B", "tenant_id", 50000101L));

    assertThat(tenantContext.getRequiredTenantId()).isEqualTo(50000101L);
  }

  @Test
  void rejectsB2cActorEvenWhenTokenContainsTenantClaim() {
    authenticate(Map.of("sub", "consumer-1", "actor_type", "B2C", "tenant_id", 50000101L));

    assertThatThrownBy(tenantContext::getRequiredTenantId)
        .isInstanceOf(TenantAccessDeniedException.class);
  }

  @Test
  void rejectsMissingTenantClaim() {
    authenticate(Map.of("sub", "user-1", "actor_type", "B2B"));

    assertThatThrownBy(tenantContext::getRequiredTenantId)
        .isInstanceOf(TenantAccessDeniedException.class);
  }

  private void authenticate(Map<String, Object> claims) {
    Jwt jwt =
        new Jwt(
            "token", Instant.now(), Instant.now().plusSeconds(300), Map.of("alg", "none"), claims);
    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
  }
}
