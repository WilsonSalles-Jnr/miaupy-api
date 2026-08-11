package com.miaupy.business.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataBusinessRepository extends JpaRepository<BusinessJpaEntity, UUID> {

    Optional<BusinessJpaEntity> findByTenantId(Long tenantId);

    Optional<BusinessJpaEntity> findBySlugIgnoreCaseAndActiveTrueAndPublicVisibleTrue(String slug);

    boolean existsByTenantId(Long tenantId);

    boolean existsBySlugIgnoreCaseAndTenantIdNot(String slug, Long tenantId);
}
