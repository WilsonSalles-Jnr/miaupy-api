package com.miaupy.scheduling.domain;

import java.time.Instant;
import java.util.UUID;

public record Appointment(
    UUID id,
    Long tenantId,
    UUID tenantCustomerId,
    UUID tenantPetId,
    UUID serviceId,
    UUID employeeId,
    String scheduleResource,
    AppointmentOrigin requestedBy,
    Instant startAt,
    Instant endAt,
    AppointmentStatus status,
    String notes,
    Instant createdAt,
    Instant updatedAt,
    Long version) {

  public static Appointment create(
      Long tenantId,
      UUID customerId,
      UUID petId,
      UUID serviceId,
      UUID employeeId,
      AppointmentOrigin origin,
      Instant startAt,
      Instant endAt,
      AppointmentStatus initialStatus,
      String notes) {
    if (!endAt.isAfter(startAt)) {
      throw new IllegalArgumentException("Appointment end must be after start");
    }
    if (initialStatus != AppointmentStatus.REQUESTED
        && initialStatus != AppointmentStatus.CONFIRMED) {
      throw new IllegalArgumentException("Invalid initial appointment status");
    }
    Instant now = Instant.now();
    return new Appointment(
        UUID.randomUUID(),
        tenantId,
        customerId,
        petId,
        serviceId,
        employeeId,
        resource(employeeId, serviceId),
        origin,
        startAt,
        endAt,
        initialStatus,
        notes,
        now,
        now,
        null);
  }

  public Appointment transitionTo(AppointmentStatus target) {
    if (!status.canTransitionTo(target)) {
      throw new InvalidAppointmentTransitionException(status, target);
    }
    return new Appointment(
        id,
        tenantId,
        tenantCustomerId,
        tenantPetId,
        serviceId,
        employeeId,
        scheduleResource,
        requestedBy,
        startAt,
        endAt,
        target,
        notes,
        createdAt,
        Instant.now(),
        version);
  }

  public static String resource(UUID employeeId, UUID serviceId) {
    return employeeId == null ? "service:" + serviceId : "employee:" + employeeId;
  }
}
