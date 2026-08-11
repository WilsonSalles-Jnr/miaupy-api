package com.miaupy.onboarding.infrastructure;

import com.miaupy.onboarding.domain.ProviderUpgrade;
import com.miaupy.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProviderUpgradePersistence {
  private final TenantJpaRepository tenants;
  private final ProviderUpgradeJpaRepository upgrades;
  private final JdbcTemplate jdbcTemplate;

  ProviderUpgradePersistence(
      TenantJpaRepository tenants,
      ProviderUpgradeJpaRepository upgrades,
      JdbcTemplate jdbcTemplate) {
    this.tenants = tenants;
    this.upgrades = upgrades;
    this.jdbcTemplate = jdbcTemplate;
  }

  public void lockSubject(String subject) {
    jdbcTemplate.queryForObject(
        "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))::text", String.class, subject);
  }

  public Long createTenant() {
    return tenants.save(new TenantJpaEntity("ACTIVE", Instant.now())).id;
  }

  public Optional<ProviderUpgrade> findBySubject(String subject) {
    return upgrades.findByAuthSubject(subject).map(ProviderUpgradeJpaEntity::toDomain);
  }

  public Optional<ProviderUpgrade> findByIdempotencyKey(UUID key) {
    return upgrades.findByIdempotencyKey(key).map(ProviderUpgradeJpaEntity::toDomain);
  }

  public ProviderUpgrade getRequired(UUID id) {
    return upgrades
        .findById(id)
        .map(ProviderUpgradeJpaEntity::toDomain)
        .orElseThrow(() -> new ResourceNotFoundException("Provider upgrade not found"));
  }

  public ProviderUpgrade save(ProviderUpgrade upgrade) {
    return upgrades.save(new ProviderUpgradeJpaEntity(upgrade)).toDomain();
  }
}
