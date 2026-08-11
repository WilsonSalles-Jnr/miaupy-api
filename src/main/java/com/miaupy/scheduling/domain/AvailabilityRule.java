package com.miaupy.scheduling.domain;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record AvailabilityRule(
    UUID id,
    Long tenantId,
    UUID employeeId,
    DayOfWeek dayOfWeek,
    LocalTime startLocal,
    LocalTime endLocal,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {
  public static AvailabilityRule create(
      Long tenantId, UUID employeeId, DayOfWeek dayOfWeek, LocalTime start, LocalTime end) {
    if (!end.isAfter(start)) {
      throw new IllegalArgumentException("Availability end must be after start");
    }
    Instant now = Instant.now();
    return new AvailabilityRule(
        UUID.randomUUID(), tenantId, employeeId, dayOfWeek, start, end, true, now, now);
  }

  public AvailabilityRule deactivate() {
    return new AvailabilityRule(
        id, tenantId, employeeId, dayOfWeek, startLocal, endLocal, false, createdAt, Instant.now());
  }
}
