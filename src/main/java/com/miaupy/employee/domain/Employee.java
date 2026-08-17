package com.miaupy.employee.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record Employee(
    UUID id,
    Long tenantId,
    String authSubject,
    String name,
    String email,
    String phone,
    EmployeeRole role,
    boolean active,
    Instant createdAt,
    Instant updatedAt,
    long version) {

  public static Employee create(
      Long tenantId,
      String authSubject,
      String name,
      String email,
      String phone,
      EmployeeRole role) {
    Instant now = Instant.now();
    return new Employee(
        UUID.randomUUID(),
        tenantId,
        authSubject,
        normalizeRequired(name),
        normalizeEmail(email),
        normalizeOptional(phone),
        role,
        true,
        now,
        now,
        0);
  }

  public Employee deactivate() {
    return new Employee(
        id,
        tenantId,
        authSubject,
        name,
        email,
        phone,
        role,
        false,
        createdAt,
        Instant.now(),
        version);
  }

  private static String normalizeRequired(String value) {
    return value.strip().replaceAll("\\s+", " ");
  }

  private static String normalizeEmail(String value) {
    return value.strip().toLowerCase(Locale.ROOT);
  }

  private static String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
