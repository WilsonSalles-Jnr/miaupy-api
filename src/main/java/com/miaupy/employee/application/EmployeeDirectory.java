package com.miaupy.employee.application;

import com.miaupy.employee.domain.Employee;
import com.miaupy.employee.domain.EmployeeRepository;
import com.miaupy.shared.exception.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class EmployeeDirectory {
  private final EmployeeRepository employees;

  public EmployeeDirectory(EmployeeRepository employees) {
    this.employees = employees;
  }

  public Employee requireActive(UUID employeeId, Long tenantId) {
    Employee employee =
        employees
            .findByIdAndTenantId(employeeId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
    if (!employee.active()) {
      throw new ResourceNotFoundException("Employee not found");
    }
    return employee;
  }
}
