package com.miaupy.scheduling.api;

import com.miaupy.scheduling.domain.Appointment;
import com.miaupy.scheduling.domain.AppointmentOrigin;
import com.miaupy.scheduling.domain.AppointmentStatus;
import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(
    UUID id,
    UUID customerId,
    UUID petId,
    UUID serviceId,
    UUID employeeId,
    AppointmentOrigin requestedBy,
    Instant startAt,
    Instant endAt,
    AppointmentStatus status,
    String notes) {
  static AppointmentResponse from(Appointment appointment) {
    return new AppointmentResponse(
        appointment.id(),
        appointment.tenantCustomerId(),
        appointment.tenantPetId(),
        appointment.serviceId(),
        appointment.employeeId(),
        appointment.requestedBy(),
        appointment.startAt(),
        appointment.endAt(),
        appointment.status(),
        appointment.notes());
  }
}
