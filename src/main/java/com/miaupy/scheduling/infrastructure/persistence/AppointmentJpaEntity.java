package com.miaupy.scheduling.infrastructure.persistence;

import com.miaupy.scheduling.domain.Appointment;
import com.miaupy.scheduling.domain.AppointmentOrigin;
import com.miaupy.scheduling.domain.AppointmentStatus;
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
@Table(name = "appointment", schema = "scheduling")
class AppointmentJpaEntity {
  @Id UUID id;

  @Column(name = "tenant_id", nullable = false)
  Long tenantId;

  @Column(name = "tenant_customer_id", nullable = false)
  UUID tenantCustomerId;

  @Column(name = "tenant_pet_id", nullable = false)
  UUID tenantPetId;

  @Column(name = "service_id", nullable = false)
  UUID serviceId;

  @Column(name = "employee_id")
  UUID employeeId;

  @Column(name = "schedule_resource", nullable = false, length = 80)
  String scheduleResource;

  @Enumerated(EnumType.STRING)
  @Column(name = "requested_by", nullable = false)
  AppointmentOrigin requestedBy;

  @Column(name = "start_at", nullable = false)
  Instant startAt;

  @Column(name = "end_at", nullable = false)
  Instant endAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  AppointmentStatus status;

  @Column(length = 2000)
  String notes;

  @Column(name = "created_at", nullable = false, updatable = false)
  Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  Instant updatedAt;

  @Version Long version;

  protected AppointmentJpaEntity() {}

  AppointmentJpaEntity(Appointment a) {
    id = a.id();
    tenantId = a.tenantId();
    tenantCustomerId = a.tenantCustomerId();
    tenantPetId = a.tenantPetId();
    serviceId = a.serviceId();
    employeeId = a.employeeId();
    scheduleResource = a.scheduleResource();
    requestedBy = a.requestedBy();
    startAt = a.startAt();
    endAt = a.endAt();
    status = a.status();
    notes = a.notes();
    createdAt = a.createdAt();
    updatedAt = a.updatedAt();
    version = a.version();
  }
}
