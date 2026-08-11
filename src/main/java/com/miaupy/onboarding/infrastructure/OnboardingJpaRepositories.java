package com.miaupy.onboarding.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TenantJpaRepository extends JpaRepository<TenantJpaEntity, Long> {}

interface ProviderUpgradeJpaRepository extends JpaRepository<ProviderUpgradeJpaEntity, UUID> {
  Optional<ProviderUpgradeJpaEntity> findByAuthSubject(String authSubject);

  Optional<ProviderUpgradeJpaEntity> findByIdempotencyKey(UUID idempotencyKey);
}
