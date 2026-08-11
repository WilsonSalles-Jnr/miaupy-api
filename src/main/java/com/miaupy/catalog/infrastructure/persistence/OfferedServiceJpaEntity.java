package com.miaupy.catalog.infrastructure.persistence;

import com.miaupy.catalog.domain.OfferedService;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service", schema = "catalog")
class OfferedServiceJpaEntity {
  @Id UUID id;

  @Column(name = "tenant_id", nullable = false)
  Long tenantId;

  @Column(nullable = false, length = 180)
  String name;

  @Column(length = 3000)
  String description;

  @Column(name = "duration_minutes", nullable = false)
  int durationMinutes;

  @Column(nullable = false, precision = 19, scale = 2)
  BigDecimal price;

  @Column(nullable = false)
  boolean active;

  @Column(nullable = false)
  boolean published;

  @Column(name = "requires_approval", nullable = false)
  boolean requiresApproval;

  @Column(name = "deleted_at")
  Instant deletedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  Instant updatedAt;

  @Version Long version;

  protected OfferedServiceJpaEntity() {}

  OfferedServiceJpaEntity(OfferedService s) {
    id = s.id();
    tenantId = s.tenantId();
    name = s.name();
    description = s.description();
    durationMinutes = s.durationMinutes();
    price = s.price();
    active = s.active();
    published = s.published();
    requiresApproval = s.requiresApproval();
    deletedAt = s.deletedAt();
    createdAt = s.createdAt();
    updatedAt = s.updatedAt();
    version = s.version();
  }
}
