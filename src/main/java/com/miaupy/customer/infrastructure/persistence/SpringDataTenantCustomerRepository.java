package com.miaupy.customer.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataTenantCustomerRepository extends JpaRepository<TenantCustomerJpaEntity, UUID> {
  Optional<TenantCustomerJpaEntity> findByIdAndTenantIdAndActiveTrue(UUID id, Long tenantId);

  Page<TenantCustomerJpaEntity> findAllByTenantIdAndActiveTrue(Long tenantId, Pageable pageable);
}
