package com.miaupy.scheduling.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AppointmentRepository {
  Appointment save(Appointment appointment);

  Optional<Appointment> findByIdAndTenantId(UUID id, Long tenantId);

  Optional<Appointment> findByIdAndCustomerId(UUID id, UUID customerId);

  Page<Appointment> findAllByTenantId(Long tenantId, Pageable pageable);

  Page<Appointment> findAllByCustomerId(UUID customerId, Pageable pageable);

  Page<Appointment> findAllByConsumerProfileId(UUID consumerProfileId, Pageable pageable);

  Optional<Appointment> findByIdAndConsumerProfileId(UUID id, UUID consumerProfileId);

  boolean hasConflict(
      Long tenantId, String scheduleResource, Instant requestedStart, Instant requestedEnd);

  List<Appointment> findOccupiedBetween(Long tenantId, Instant start, Instant end);

  List<Appointment> findConfirmedStartingBetween(Instant start, Instant end);
}
