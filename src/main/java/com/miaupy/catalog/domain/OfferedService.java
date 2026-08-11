package com.miaupy.catalog.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

public record OfferedService(
    UUID id,
    Long tenantId,
    String name,
    String description,
    int durationMinutes,
    BigDecimal price,
    boolean active,
    boolean published,
    boolean requiresApproval,
    Instant deletedAt,
    Instant createdAt,
    Instant updatedAt,
    Long version) {
  public static OfferedService create(
      Long tenantId,
      String name,
      String description,
      int durationMinutes,
      BigDecimal price,
      boolean requiresApproval) {
    validateDuration(durationMinutes);
    Instant now = Instant.now();
    return new OfferedService(
        UUID.randomUUID(),
        tenantId,
        name,
        description,
        durationMinutes,
        money(price),
        true,
        false,
        requiresApproval,
        null,
        now,
        now,
        null);
  }

  public OfferedService update(
      String name,
      String description,
      int durationMinutes,
      BigDecimal price,
      boolean requiresApproval) {
    validateDuration(durationMinutes);
    return new OfferedService(
        id,
        tenantId,
        name,
        description,
        durationMinutes,
        money(price),
        active,
        published,
        requiresApproval,
        deletedAt,
        createdAt,
        Instant.now(),
        version);
  }

  public OfferedService publish() {
    if (!active) {
      throw new IllegalArgumentException("Inactive service cannot be published");
    }
    return copy(true, active, deletedAt);
  }

  public OfferedService unpublish() {
    return copy(false, active, deletedAt);
  }

  public OfferedService deactivate() {
    return copy(false, false, Instant.now());
  }

  private OfferedService copy(boolean published, boolean active, Instant deletedAt) {
    return new OfferedService(
        id,
        tenantId,
        name,
        description,
        durationMinutes,
        price,
        active,
        published,
        requiresApproval,
        deletedAt,
        createdAt,
        Instant.now(),
        version);
  }

  private static void validateDuration(int durationMinutes) {
    if (durationMinutes <= 0) {
      throw new IllegalArgumentException("Service duration must be greater than zero");
    }
  }

  private static BigDecimal money(BigDecimal value) {
    if (value == null || value.signum() <= 0) {
      throw new IllegalArgumentException("Service price must be greater than zero");
    }
    return value.setScale(2, RoundingMode.UNNECESSARY);
  }
}
