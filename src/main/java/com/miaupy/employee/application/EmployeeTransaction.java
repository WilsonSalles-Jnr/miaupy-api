package com.miaupy.employee.application;

import com.miaupy.employee.domain.Employee;
import com.miaupy.employee.domain.EmployeeRepository;
import com.miaupy.employee.domain.EmployeeRole;
import com.miaupy.integration.outbox.OutboxWriter;
import com.miaupy.shared.exception.ResourceNotFoundException;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeTransaction {
  private final EmployeeRepository employees;
  private final OutboxWriter outbox;

  public EmployeeTransaction(EmployeeRepository employees, OutboxWriter outbox) {
    this.employees = employees;
    this.outbox = outbox;
  }

  @Transactional
  public Employee create(
      Long tenantId,
      String authSubject,
      String name,
      String email,
      String phone,
      EmployeeRole role) {
    Employee employee =
        employees.save(Employee.create(tenantId, authSubject, name, email, phone, role));
    outbox.append(
        "Employee",
        employee.id(),
        "employee.created",
        tenantId,
        Map.of("email", employee.email(), "role", employee.role().name()));
    return employee;
  }

  @Transactional
  public Employee deactivate(UUID id, Long tenantId) {
    Employee employee =
        employees
            .findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
    Employee saved = employees.save(employee.deactivate());
    outbox.append(
        "Employee",
        saved.id(),
        "employee.deactivated",
        tenantId,
        Map.of("role", saved.role().name()));
    return saved;
  }
}
