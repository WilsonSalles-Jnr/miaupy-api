package com.miaupy.shared.security;

public interface ActorContext {

  AuthenticatedActor getRequiredActor();

  String getRequiredActorDisplayName();

  String getRequiredConsumerSubject();

  ConsumerIdentity getRequiredVerifiedConsumerIdentity();

  boolean isEmailVerified();
}
