package com.miaupy.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miaupy.shared.exception.ActorAccessDeniedException;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SecurityContextActorContextTest {
  private final SecurityContextActorContext actorContext = new SecurityContextActorContext();

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void readsStableSubjectFromJwt() {
    authenticate(jwt("consumer-123"));

    AuthenticatedActor actor = actorContext.getRequiredActor();

    assertThat(actor.subject()).isEqualTo("consumer-123");
    assertThat(actor.type()).isEqualTo(ActorType.B2C);
  }

  @Test
  void rejectsTokenWithoutSubject() {
    authenticate(jwt(null));

    assertThatThrownBy(actorContext::getRequiredActor)
        .isInstanceOf(ActorAccessDeniedException.class)
        .hasMessage("JWT sub claim is required");
  }

  @Test
  void exposesVerifiedConsumerClaimsForProvisioning() {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .subject("consumer-123")
            .claim("actor_type", "B2C")
            .claim("email_verified", true)
            .claim("email", "jane@example.com")
            .claim("name", "Jane Doe")
            .build();
    authenticate(jwt);

    ConsumerIdentity identity = actorContext.getRequiredVerifiedConsumerIdentity();

    assertThat(identity.subject()).isEqualTo("consumer-123");
    assertThat(identity.name()).isEqualTo("Jane Doe");
    assertThat(identity.email()).isEqualTo("jane@example.com");
  }

  @Test
  void allowsBusinessActorToUseConsumerSelfServiceWithTheSameSubject() {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .subject("business-owner-123")
            .claim("actor_type", "B2B")
            .claim("tenant_id", 50000101L)
            .claim("email_verified", true)
            .claim("email", "owner@example.com")
            .claim("name", "Business Owner")
            .build();
    authenticate(jwt);

    ConsumerIdentity identity = actorContext.getRequiredVerifiedConsumerIdentity();

    assertThat(identity.subject()).isEqualTo("business-owner-123");
    assertThat(identity.email()).isEqualTo("owner@example.com");
  }

  private void authenticate(Jwt jwt) {
    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, java.util.List.of(), "test-principal"));
  }

  private Jwt jwt(String subject) {
    Jwt.Builder builder =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .claim("actor_type", "B2C");
    if (subject != null) {
      builder.subject(subject);
    }
    return builder.build();
  }
}
