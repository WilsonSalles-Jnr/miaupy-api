package com.miaupy.onboarding.domain;

import java.time.Instant;
import java.util.UUID;

public record BusinessRegistration(
    UUID id,
    String authSubject,
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
