package com.miaupy.onboarding.infrastructure;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(schema = "platform", name = "tenant")
class TenantJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tenant_id_generator")
  @SequenceGenerator(
      name = "tenant_id_generator",
      sequenceName = "platform.tenant_id_seq",
      allocationSize = 1)
  Long id;

  String status;
  Instant createdAt;

  protected TenantJpaEntity() {}

  TenantJpaEntity(String status, Instant createdAt) {
    this.status = status;
    this.createdAt = createdAt;
  }
}
