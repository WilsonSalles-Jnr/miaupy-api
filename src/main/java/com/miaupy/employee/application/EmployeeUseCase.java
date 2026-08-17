package com.miaupy.employee.application;

import com.miaupy.employee.domain.Employee;
import com.miaupy.employee.domain.EmployeeRepository;
import com.miaupy.employee.domain.EmployeeRole;
import com.miaupy.onboarding.domain.IdentityProvider;
import com.miaupy.shared.exception.ConflictException;
import com.miaupy.shared.exception.ResourceNotFoundException;
import com.miaupy.shared.tenant.TenantContext;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeUseCase {
  private final TenantContext tenants;
  private final EmployeeRepository employees;
  private final IdentityProvider identities;
  private final EmployeeTransaction transaction;

  public EmployeeUseCase(
      TenantContext tenants,
      EmployeeRepository employees,
      IdentityProvider identities,
      EmployeeTransaction transaction) {
    this.tenants = tenants;
    this.employees = employees;
    this.identities = identities;
    this.transaction = transaction;
  }

  public Employee create(Command command) {
    Long tenantId = tenants.getRequiredTenantId();
    String email = command.email().strip().toLowerCase(Locale.ROOT);
    if (employees.existsByTenantIdAndEmail(tenantId, email)) {
      throw new ConflictException("An employee with this email already exists in the business");
    }

    IdentityProvider.RegistrationResult registration =
        identities.registerEmployee(
            command.name(), email, command.temporaryPassword(), tenantId, command.role());
    String subject =
        registration
            .createdSubject()
            .orElseThrow(
                () -> new ConflictException("This email is already used by another identity"));
    try {
      return transaction.create(
          tenantId, subject, command.name(), email, command.phone(), command.role());
    } catch (RuntimeException exception) {
      identities.deleteUnverifiedUser(subject);
      throw exception;
    }
  }

  @Transactional(readOnly = true)
  public Page<Employee> list(int page, int size) {
    return employees.findAllByTenantId(
        tenants.getRequiredTenantId(),
        PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.ASC, "name")));
  }

  @Transactional(readOnly = true)
  public Employee get(UUID id) {
    return required(id, tenants.getRequiredTenantId());
  }

  public void deactivate(UUID id) {
    Long tenantId = tenants.getRequiredTenantId();
    Employee current = required(id, tenantId);
    identities.setUserEnabled(current.authSubject(), false);
    transaction.deactivate(id, tenantId);
  }

  private Employee required(UUID id, Long tenantId) {
    return employees
        .findByIdAndTenantId(id, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
  }

  public record Command(
      String name, String email, String phone, String temporaryPassword, EmployeeRole role) {}
}
