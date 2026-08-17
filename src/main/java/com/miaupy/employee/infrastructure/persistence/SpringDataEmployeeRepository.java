package com.miaupy.employee.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataEmployeeRepository extends JpaRepository<EmployeeJpaEntity, UUID> {
  boolean existsByTenantIdAndEmailIgnoreCase(Long tenantId, String email);

  Optional<EmployeeJpaEntity> findByIdAndTenantId(UUID id, Long tenantId);

  Page<EmployeeJpaEntity> findAllByTenantId(Long tenantId, Pageable pageable);
}
