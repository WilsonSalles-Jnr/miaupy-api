package com.miaupy.onboarding.infrastructure;

import com.miaupy.onboarding.domain.BusinessRegistration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "platform", name = "business_registration")
class BusinessRegistrationJpaEntity {
  @Id UUID id;

  @Column(name = "auth_subject", nullable = false, unique = true, length = 160)
  String authSubject;

  @Column(name = "idempotency_key", nullable = false, unique = true)
  UUID idempotencyKey;

  @Column(name = "request_fingerprint", nullable = false, length = 64)
  String requestFingerprint;

  @Column(name = "tenant_id", nullable = false)
  Long tenantId;

  @Column(name = "business_id", nullable = false)
  UUID businessId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  BusinessRegistration.Status status;

  @Column(name = "created_at", nullable = false)
  Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  Instant updatedAt;

  protected BusinessRegistrationJpaEntity() {}

  BusinessRegistrationJpaEntity(BusinessRegistration registration) {
    id = registration.id();
    authSubject = registration.authSubject();
    idempotencyKey = registration.idempotencyKey();
    requestFingerprint = registration.requestFingerprint();
    tenantId = registration.tenantId();
    businessId = registration.businessId();
    status = registration.status();
    createdAt = registration.createdAt();
    updatedAt = registration.updatedAt();
  }

  BusinessRegistration toDomain() {
    return new BusinessRegistration(
        id,
        authSubject,
        idempotencyKey,
        requestFingerprint,
        tenantId,
        businessId,
        status,
        createdAt,
        updatedAt);
  }
}
