package com.miaupy.employee.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeRepository {
  Employee save(Employee employee);

  boolean existsByTenantIdAndEmail(Long tenantId, String email);

  Optional<Employee> findByIdAndTenantId(UUID id, Long tenantId);

  Page<Employee> findAllByTenantId(Long tenantId, Pageable pageable);
}
