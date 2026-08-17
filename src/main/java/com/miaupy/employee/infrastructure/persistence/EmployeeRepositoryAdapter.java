package com.miaupy.employee.infrastructure.persistence;

import com.miaupy.employee.domain.Employee;
import com.miaupy.employee.domain.EmployeeRepository;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
class EmployeeRepositoryAdapter implements EmployeeRepository {
  private final SpringDataEmployeeRepository repository;

  EmployeeRepositoryAdapter(SpringDataEmployeeRepository repository) {
    this.repository = repository;
  }

  @Override
  public Employee save(Employee employee) {
    return repository.save(new EmployeeJpaEntity(employee)).toDomain();
  }

  @Override
  public boolean existsByTenantIdAndEmail(Long tenantId, String email) {
    return repository.existsByTenantIdAndEmailIgnoreCase(
        tenantId, email.strip().toLowerCase(Locale.ROOT));
  }

  @Override
  public Optional<Employee> findByIdAndTenantId(UUID id, Long tenantId) {
    return repository.findByIdAndTenantId(id, tenantId).map(EmployeeJpaEntity::toDomain);
  }

  @Override
  public Page<Employee> findAllByTenantId(Long tenantId, Pageable pageable) {
    return repository.findAllByTenantId(tenantId, pageable).map(EmployeeJpaEntity::toDomain);
  }
}
