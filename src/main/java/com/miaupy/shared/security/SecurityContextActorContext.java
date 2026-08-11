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
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new ActorAccessDeniedException("Authenticated JWT actor is required");
        }

        ActorType actorType;
        try {
            actorType = ActorType.valueOf(jwt.getClaimAsString("actor_type"));
        } catch (RuntimeException exception) {
            throw new ActorAccessDeniedException("JWT actor_type must be B2B or B2C");
        }

        AuthenticatedActor actor = new AuthenticatedActor(jwt.getSubject(), actorType);
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
}
