package com.miaupy.shared.security;

import com.miaupy.shared.exception.ActorAccessDeniedException;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextActorContext implements ActorContext {

  @Override
  public AuthenticatedActor getRequiredActor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof Jwt jwt)) {
      throw new ActorAccessDeniedException("Authenticated JWT actor is required");
    }

    ActorType actorType;
    try {
      actorType = ActorType.valueOf(jwt.getClaimAsString("actor_type"));
    } catch (RuntimeException exception) {
      throw new ActorAccessDeniedException("JWT actor_type must be B2B or B2C");
    }

    String subject = jwt.getSubject();
    if (subject == null || subject.isBlank()) {
      throw new ActorAccessDeniedException("JWT sub claim is required");
    }

    AuthenticatedActor actor = new AuthenticatedActor(subject, actorType);
    MDC.put("actorId", actor.subject());
    return actor;
  }

  @Override
  public String getRequiredConsumerSubject() {
    AuthenticatedActor actor = getRequiredActor();
    if (actor.type() != ActorType.B2C) {
      throw new ActorAccessDeniedException("A B2C actor is required");
    }
    return actor.subject();
  }

  @Override
  public ConsumerIdentity getRequiredVerifiedConsumerIdentity() {
    String subject = getRequiredConsumerSubject();
    Jwt jwt = getRequiredJwt();
    if (!Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
      throw new ActorAccessDeniedException("A verified email is required");
    }
    String email = jwt.getClaimAsString("email");
    if (email == null || email.isBlank()) {
      throw new ActorAccessDeniedException("JWT email claim is required");
    }
    String name = jwt.getClaimAsString("name");
    if (name == null || name.isBlank()) {
      name = jwt.getClaimAsString("preferred_username");
    }
    if (name == null || name.isBlank()) {
      name = email;
    }
    return new ConsumerIdentity(subject, name, email);
  }

  @Override
  public boolean isEmailVerified() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.getPrincipal() instanceof Jwt jwt
        && Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"));
  }

  private Jwt getRequiredJwt() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof Jwt jwt)) {
      throw new ActorAccessDeniedException("Authenticated JWT actor is required");
    }
    return jwt;
  }
}
