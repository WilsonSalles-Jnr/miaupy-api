package com.miaupy.onboarding.application;

import com.miaupy.business.domain.AppointmentApprovalMode;
import com.miaupy.business.domain.Business;
import com.miaupy.business.domain.BusinessConfigurationRepository;
import com.miaupy.business.domain.BusinessRepository;
import com.miaupy.business.domain.BusinessSettings;
import com.miaupy.integration.outbox.OutboxWriter;
import com.miaupy.onboarding.domain.BusinessRegistration;
import com.miaupy.onboarding.infrastructure.BusinessRegistrationPersistence;
import com.miaupy.shared.exception.ConflictException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessRegistrationTransaction {
  private final BusinessRepository businesses;
  private final BusinessConfigurationRepository configurations;
  private final BusinessRegistrationPersistence persistence;
  private final OutboxWriter outbox;

  public BusinessRegistrationTransaction(
      BusinessRepository businesses,
      BusinessConfigurationRepository configurations,
      BusinessRegistrationPersistence persistence,
      OutboxWriter outbox) {
    this.businesses = businesses;
    this.configurations = configurations;
    this.persistence = persistence;
    this.outbox = outbox;
  }

  @Transactional(readOnly = true)
  public Optional<BusinessRegistration> find(UUID idempotencyKey) {
    return persistence.findByIdempotencyKey(idempotencyKey);
  }

  @Transactional
  public BusinessRegistration prepare(
      String subject,
      UUID idempotencyKey,
      String fingerprint,
      BusinessRegistrationCommand command) {
    BusinessRegistration existing = persistence.findByIdempotencyKey(idempotencyKey).orElse(null);
    if (existing != null) {
      ensureSameRequest(existing, fingerprint);
      return existing;
    }

    Long tenantId = persistence.createTenant();
    String slug = command.slug().strip().toLowerCase(java.util.Locale.ROOT);
    if (businesses.existsBySlugAndDifferentTenant(slug, tenantId)) {
      throw new ConflictException("The requested public slug is already in use");
    }
    Business business =
        businesses.save(
            Business.create(
                tenantId,
                slug,
                command.name(),
                command.tradeName(),
                command.document(),
                command.description(),
                command.phone(),
                command.email(),
                command.website()));
    configurations.saveSettings(
        BusinessSettings.create(
            tenantId, AppointmentApprovalMode.MANUAL, "America/Sao_Paulo", "BRL", false, false));
    Instant now = Instant.now();
    return persistence.save(
        new BusinessRegistration(
            UUID.randomUUID(),
            subject,
            idempotencyKey,
            fingerprint,
            tenantId,
            business.id(),
            BusinessRegistration.Status.LOCAL_READY,
            now,
            now));
  }

  @Transactional
  public BusinessRegistration complete(UUID id) {
    BusinessRegistration current = persistence.getRequired(id);
    if (current.status() == BusinessRegistration.Status.COMPLETED) return current;

    BusinessRegistration completed =
        new BusinessRegistration(
            current.id(),
            current.authSubject(),
            current.idempotencyKey(),
            current.requestFingerprint(),
            current.tenantId(),
            current.businessId(),
            BusinessRegistration.Status.COMPLETED,
            current.createdAt(),
            Instant.now());
    BusinessRegistration saved = persistence.save(completed);
    outbox.appendSystem(
        "BusinessRegistration",
        saved.id(),
        "business.registered",
        saved.tenantId(),
        saved.authSubject(),
        Map.of("businessId", saved.businessId()));
    return saved;
  }

  public void ensureSameRequest(BusinessRegistration registration, String fingerprint) {
    if (!registration.requestFingerprint().equals(fingerprint)) {
      throw new ConflictException("Idempotency-Key was already used by another request");
    }
  }
}
