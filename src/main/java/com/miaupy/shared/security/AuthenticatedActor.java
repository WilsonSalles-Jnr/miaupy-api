package com.miaupy.shared.security;

public record AuthenticatedActor(String subject, ActorType type) {
}
