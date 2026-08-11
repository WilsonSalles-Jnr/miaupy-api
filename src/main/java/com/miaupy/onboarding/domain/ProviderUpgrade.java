package com.miaupy.onboarding.domain;

import java.time.Instant;
import java.util.UUID;

public record ProviderUpgrade(
    UUID id,
    String authSubject,
    UUID consumerProfileId,
    UUID idempotencyKey,
    String requestFingerprint,
    Long tenantId,
    UUID businessId,
    Status status,
    Instant createdAt,
    Instant updatedAt) {

  public enum Status {
    LOCAL_READY,
    COMPLETED
  }
}
