package com.miaupy.onboarding.infrastructure;

import com.miaupy.onboarding.domain.ProviderUpgrade;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "platform", name = "provider_upgrade")
class ProviderUpgradeJpaEntity {
  @Id UUID id;
  String authSubject;
  UUID consumerProfileId;
  UUID idempotencyKey;

  @JdbcTypeCode(SqlTypes.CHAR)
  @jakarta.persistence.Column(length = 64, columnDefinition = "char(64)")
  String requestFingerprint;

  Long tenantId;
  UUID businessId;

  @Enumerated(EnumType.STRING)
  ProviderUpgrade.Status status;

  Instant createdAt;
  Instant updatedAt;

  protected ProviderUpgradeJpaEntity() {}

  ProviderUpgradeJpaEntity(ProviderUpgrade upgrade) {
    id = upgrade.id();
    authSubject = upgrade.authSubject();
    consumerProfileId = upgrade.consumerProfileId();
    idempotencyKey = upgrade.idempotencyKey();
    requestFingerprint = upgrade.requestFingerprint();
    tenantId = upgrade.tenantId();
    businessId = upgrade.businessId();
    status = upgrade.status();
    createdAt = upgrade.createdAt();
    updatedAt = upgrade.updatedAt();
  }

  ProviderUpgrade toDomain() {
    return new ProviderUpgrade(
        id,
        authSubject,
        consumerProfileId,
        idempotencyKey,
        requestFingerprint,
        tenantId,
        businessId,
        status,
        createdAt,
        updatedAt);
  }
}
