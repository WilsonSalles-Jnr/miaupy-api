package com.miaupy.customer.domain;

import java.time.Instant;
import java.util.UUID;

public record TenantCustomer(
    UUID id,
    Long tenantId,
    UUID consumerProfileId,
    String name,
    String email,
    String phone,
    String document,
    String notes,
    boolean active,
    Instant deletedAt,
    Instant createdAt,
    Instant updatedAt,
    Long version) {
  public static TenantCustomer create(
      Long tenantId, String name, String email, String phone, String document, String notes) {
    Instant now = Instant.now();
    return new TenantCustomer(
        UUID.randomUUID(),
        tenantId,
        null,
        name,
        email,
        phone,
        document,
        notes,
        true,
        null,
        now,
        now,
        null);
  }

  public TenantCustomer update(
      String name, String email, String phone, String document, String notes) {
    return new TenantCustomer(
        id,
        tenantId,
        consumerProfileId,
        name,
        email,
        phone,
        document,
        notes,
        active,
        deletedAt,
        createdAt,
        Instant.now(),
        version);
  }

  public TenantCustomer deactivate() {
    Instant now = Instant.now();
    return new TenantCustomer(
        id,
        tenantId,
        consumerProfileId,
        name,
        email,
        phone,
        document,
        notes,
        false,
        now,
        createdAt,
        now,
        version);
  }
}
