package com.miaupy.shared.security;

public interface ActorContext {

  AuthenticatedActor getRequiredActor();

  String getRequiredConsumerSubject();

  boolean isEmailVerified();
}
