package com.miaupy.onboarding.application;

import com.miaupy.business.domain.AppointmentApprovalMode;
import com.miaupy.business.domain.Business;
import com.miaupy.business.domain.BusinessConfigurationRepository;
import com.miaupy.business.domain.BusinessRepository;
import com.miaupy.business.domain.BusinessSettings;
import com.miaupy.consumer.domain.ConsumerProfile;
import com.miaupy.consumer.domain.ConsumerProfileRepository;
import com.miaupy.integration.outbox.OutboxWriter;
import com.miaupy.onboarding.domain.ProviderUpgrade;
import com.miaupy.onboarding.infrastructure.ProviderUpgradePersistence;
import com.miaupy.shared.exception.ConflictException;
import com.miaupy.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderUpgradeTransaction {
  private final ConsumerProfileRepository consumers;
  private final BusinessRepository businesses;
  private final BusinessConfigurationRepository configurations;
  private final ProviderUpgradePersistence persistence;
  private final OutboxWriter outbox;

  public ProviderUpgradeTransaction(
      ConsumerProfileRepository consumers,
      BusinessRepository businesses,
      BusinessConfigurationRepository configurations,
      ProviderUpgradePersistence persistence,
      OutboxWriter outbox) {
    this.consumers = consumers;
    this.businesses = businesses;
    this.configurations = configurations;
    this.persistence = persistence;
    this.outbox = outbox;
  }

  @Transactional
  public ProviderUpgrade prepare(
      String subject, UUID idempotencyKey, String fingerprint, ProviderUpgradeCommand command) {
    persistence.lockSubject(subject);
    ProviderUpgrade existing = persistence.findBySubject(subject).orElse(null);
    if (existing != null) {
      ensureSameRequest(existing, idempotencyKey, fingerprint);
      return existing;
    }
    persistence
        .findByIdempotencyKey(idempotencyKey)
        .ifPresent(
            ignored -> {
              throw new ConflictException("Idempotency-Key was already used by another request");
            });
    ConsumerProfile consumer =
        consumers
            .findByAuthSubject(subject)
            .orElseThrow(() -> new ResourceNotFoundException("Consumer profile not found"));
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
        new ProviderUpgrade(
            UUID.randomUUID(),
            subject,
            consumer.id(),
            idempotencyKey,
            fingerprint,
            tenantId,
            business.id(),
            ProviderUpgrade.Status.LOCAL_READY,
            now,
            now));
  }

  @Transactional
  public ProviderUpgrade complete(UUID id) {
    ProviderUpgrade current = persistence.getRequired(id);
    if (current.status() == ProviderUpgrade.Status.COMPLETED) {
      return current;
    }
    ProviderUpgrade completed =
        new ProviderUpgrade(
            current.id(),
            current.authSubject(),
            current.consumerProfileId(),
            current.idempotencyKey(),
            current.requestFingerprint(),
            current.tenantId(),
            current.businessId(),
            ProviderUpgrade.Status.COMPLETED,
            current.createdAt(),
            Instant.now());
    ProviderUpgrade saved = persistence.save(completed);
    outbox.append(
        "ProviderUpgrade",
        saved.id(),
        "provider.upgraded",
        saved.tenantId(),
        Map.of("businessId", saved.businessId(), "consumerProfileId", saved.consumerProfileId()));
    return saved;
  }

  private void ensureSameRequest(
      ProviderUpgrade existing, UUID idempotencyKey, String requestFingerprint) {
    if (!existing.idempotencyKey().equals(idempotencyKey)
        || !existing.requestFingerprint().equals(requestFingerprint)) {
      throw new ConflictException("This consumer already has a different provider upgrade");
    }
  }
}
