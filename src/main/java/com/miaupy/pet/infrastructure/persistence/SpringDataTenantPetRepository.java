package com.miaupy.pet.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataTenantPetRepository extends JpaRepository<TenantPetJpaEntity,UUID>{
    Optional<TenantPetJpaEntity> findByIdAndTenantIdAndActiveTrue(UUID id,Long tenantId);
    Page<TenantPetJpaEntity> findAllByTenantCustomerIdAndTenantIdAndActiveTrue(UUID customerId,Long tenantId,Pageable pageable);
}
