package com.miaupy.scheduling.application;

import com.miaupy.catalog.domain.OfferedService;
import com.miaupy.catalog.domain.OfferedServiceRepository;
import com.miaupy.customer.domain.TenantCustomerRepository;
import com.miaupy.integration.outbox.OutboxWriter;
import com.miaupy.pet.domain.TenantPet;
import com.miaupy.pet.domain.TenantPetRepository;
import com.miaupy.scheduling.domain.Appointment;
import com.miaupy.scheduling.domain.AppointmentConflictException;
import com.miaupy.scheduling.domain.AppointmentOrigin;
import com.miaupy.scheduling.domain.AppointmentRepository;
import com.miaupy.scheduling.domain.AppointmentStatus;
import com.miaupy.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentTransaction {
  private final AppointmentRepository appointments;
  private final TenantCustomerRepository customers;
  private final TenantPetRepository pets;
  private final OfferedServiceRepository services;
  private final OutboxWriter outbox;

  public AppointmentTransaction(
      AppointmentRepository appointments,
      TenantCustomerRepository customers,
      TenantPetRepository pets,
      OfferedServiceRepository services,
      OutboxWriter outbox) {
    this.appointments = appointments;
    this.customers = customers;
    this.pets = pets;
    this.services = services;
    this.outbox = outbox;
  }

  @Transactional
  public Appointment create(CreateCommand command) {
    customers
        .findByIdAndTenantId(command.customerId(), command.tenantId())
        .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    TenantPet pet =
        pets.findByIdAndTenantId(command.petId(), command.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));
    if (!pet.tenantCustomerId().equals(command.customerId())) {
      throw new ResourceNotFoundException("Pet not found for customer");
    }
    OfferedService service =
        services
            .findByIdAndTenantId(command.serviceId(), command.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
    if (!command.startAt().isAfter(Instant.now())) {
      throw new IllegalArgumentException("Appointment start must be in the future");
    }
    Instant endAt = command.startAt().plus(service.durationMinutes(), ChronoUnit.MINUTES);
    String resource = Appointment.resource(command.employeeId(), command.serviceId());
    if (appointments.hasConflict(command.tenantId(), resource, command.startAt(), endAt)) {
      throw new AppointmentConflictException();
    }
    Appointment saved =
        appointments.save(
            Appointment.create(
                command.tenantId(),
                command.customerId(),
                command.petId(),
                command.serviceId(),
                command.employeeId(),
                command.origin(),
                command.startAt(),
                endAt,
                command.initialStatus(),
                command.notes()));
    append(saved, eventType(saved.status()));
    return saved;
  }

  @Transactional
  public Appointment transitionBusiness(UUID id, Long tenantId, AppointmentStatus target) {
    Appointment appointment =
        appointments
            .findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
    Appointment saved = appointments.save(appointment.transitionTo(target));
    append(saved, eventType(target));
    return saved;
  }

  @Transactional
  public Appointment cancelConsumer(UUID id, UUID consumerProfileId) {
    Appointment appointment =
        appointments
            .findByIdAndConsumerProfileId(id, consumerProfileId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
    Appointment saved = appointments.save(appointment.transitionTo(AppointmentStatus.CANCELLED));
    append(saved, "appointment.cancelled");
    return saved;
  }

  private void append(Appointment appointment, String type) {
    outbox.append(
        "Appointment",
        appointment.id(),
        type,
        appointment.tenantId(),
        Map.of(
            "appointmentId", appointment.id(),
            "status", appointment.status(),
            "startAt", appointment.startAt(),
            "endAt", appointment.endAt(),
            "serviceId", appointment.serviceId()));
  }

  private String eventType(AppointmentStatus status) {
    return "appointment." + status.name().toLowerCase();
  }

  public record CreateCommand(
      Long tenantId,
      UUID customerId,
      UUID petId,
      UUID serviceId,
      UUID employeeId,
      AppointmentOrigin origin,
      AppointmentStatus initialStatus,
      Instant startAt,
      String notes) {}
}
