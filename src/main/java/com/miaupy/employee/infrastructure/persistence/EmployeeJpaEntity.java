package com.miaupy.employee.infrastructure.persistence;

import com.miaupy.employee.domain.Employee;
import com.miaupy.employee.domain.EmployeeRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employee", schema = "platform")
class EmployeeJpaEntity {
  @Id UUID id;

  @Column(name = "tenant_id", nullable = false)
  Long tenantId;

  @Column(name = "auth_subject", nullable = false, length = 160)
  String authSubject;

  @Column(nullable = false, length = 160)
  String name;

  @Column(nullable = false, length = 254)
  String email;

  @Column(length = 32)
  String phone;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  EmployeeRole role;

  @Column(nullable = false)
  boolean active;

  @Column(name = "created_at", nullable = false)
  Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  Instant updatedAt;

  @Version long version;

  protected EmployeeJpaEntity() {}

  EmployeeJpaEntity(Employee employee) {
    id = employee.id();
    tenantId = employee.tenantId();
    authSubject = employee.authSubject();
    name = employee.name();
    email = employee.email();
    phone = employee.phone();
    role = employee.role();
    active = employee.active();
    createdAt = employee.createdAt();
    updatedAt = employee.updatedAt();
    version = employee.version();
  }

  Employee toDomain() {
    return new Employee(
        id, tenantId, authSubject, name, email, phone, role, active, createdAt, updatedAt, version);
  }
}
