package com.miaupy.employee.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miaupy.employee.domain.Employee;
import com.miaupy.employee.domain.EmployeeRepository;
import com.miaupy.employee.domain.EmployeeRole;
import com.miaupy.onboarding.domain.IdentityProvider;
import com.miaupy.shared.exception.ResourceNotFoundException;
import com.miaupy.shared.tenant.TenantContext;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeUseCaseTest {
  @Mock TenantContext tenants;
  @Mock EmployeeRepository employees;
  @Mock IdentityProvider identities;
  @Mock EmployeeTransaction transaction;

  private EmployeeUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new EmployeeUseCase(tenants, employees, identities, transaction);
  }

  @Test
  void createsEmployeeIdentityInsideAuthenticatedTenant() {
    Long tenantId = 50000101L;
    EmployeeUseCase.Command command =
        new EmployeeUseCase.Command(
            "Jane Vet",
            " JANE@EXAMPLE.COM ",
            "11999999999",
            "temporary pass 123",
            EmployeeRole.VETERINARIAN);
    Employee expected =
        Employee.create(
            tenantId,
            "employee-subject",
            "Jane Vet",
            "jane@example.com",
            "11999999999",
            EmployeeRole.VETERINARIAN);
    when(tenants.getRequiredTenantId()).thenReturn(tenantId);
    when(identities.registerEmployee(
            "Jane Vet",
            "jane@example.com",
            "temporary pass 123",
            tenantId,
            EmployeeRole.VETERINARIAN))
        .thenReturn(new IdentityProvider.RegistrationResult(true, "employee-subject"));
    when(transaction.create(
            tenantId,
            "employee-subject",
            "Jane Vet",
            "jane@example.com",
            "11999999999",
            EmployeeRole.VETERINARIAN))
        .thenReturn(expected);

    Employee created = useCase.create(command);

    assertThat(created).isEqualTo(expected);
  }

  @Test
  void tenantCannotReadEmployeeFromAnotherTenant() {
    UUID employeeId = UUID.randomUUID();
    when(tenants.getRequiredTenantId()).thenReturn(50000202L);
    when(employees.findByIdAndTenantId(employeeId, 50000202L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.get(employeeId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Employee not found");
  }

  @Test
  void deactivationBlocksIdentityBeforePersistingStatus() {
    Long tenantId = 50000101L;
    Employee employee =
        Employee.create(
            tenantId,
            "employee-subject",
            "Jane Vet",
            "jane@example.com",
            null,
            EmployeeRole.VETERINARIAN);
    when(tenants.getRequiredTenantId()).thenReturn(tenantId);
    when(employees.findByIdAndTenantId(employee.id(), tenantId)).thenReturn(Optional.of(employee));

    useCase.deactivate(employee.id());

    verify(identities).setUserEnabled("employee-subject", false);
    verify(transaction).deactivate(employee.id(), tenantId);
  }
}
