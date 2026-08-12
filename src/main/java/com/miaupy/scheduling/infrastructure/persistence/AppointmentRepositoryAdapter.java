package com.miaupy.scheduling.infrastructure.persistence;

import com.miaupy.scheduling.domain.Appointment;
import com.miaupy.scheduling.domain.AppointmentRepository;
import com.miaupy.scheduling.domain.AppointmentStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

interface SpringDataAppointmentRepository extends JpaRepository<AppointmentJpaEntity, UUID> {
  Optional<AppointmentJpaEntity> findByIdAndTenantId(UUID id, Long tenantId);

  Optional<AppointmentJpaEntity> findByIdAndTenantCustomerId(UUID id, UUID customerId);

  Page<AppointmentJpaEntity> findAllByTenantId(Long tenantId, Pageable pageable);

  Page<AppointmentJpaEntity> findAllByTenantCustomerId(UUID customerId, Pageable pageable);

  @Query(
      value =
          "SELECT a.* FROM scheduling.appointment a JOIN crm.tenant_customer c "
              + "ON c.id = a.tenant_customer_id AND c.tenant_id = a.tenant_id "
              + "WHERE c.consumer_profile_id = :profileId",
      countQuery =
          "SELECT count(*) FROM scheduling.appointment a JOIN crm.tenant_customer c "
              + "ON c.id = a.tenant_customer_id AND c.tenant_id = a.tenant_id "
              + "WHERE c.consumer_profile_id = :profileId",
      nativeQuery = true)
  Page<AppointmentJpaEntity> findAllForConsumerProfile(
      @Param("profileId") UUID profileId, Pageable pageable);

  @Query(
      value =
          "SELECT a.* FROM scheduling.appointment a JOIN crm.tenant_customer c "
              + "ON c.id = a.tenant_customer_id AND c.tenant_id = a.tenant_id "
              + "WHERE a.id = :id AND c.consumer_profile_id = :profileId",
      nativeQuery = true)
  Optional<AppointmentJpaEntity> findByIdForConsumerProfile(
      @Param("id") UUID id, @Param("profileId") UUID profileId);

  boolean existsByTenantIdAndScheduleResourceAndStatusInAndStartAtLessThanAndEndAtGreaterThan(
      Long tenantId,
      String scheduleResource,
      List<AppointmentStatus> statuses,
      Instant requestedEnd,
      Instant requestedStart);

  List<AppointmentJpaEntity> findAllByTenantIdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(
      Long tenantId, List<AppointmentStatus> statuses, Instant rangeEnd, Instant rangeStart);

  List<AppointmentJpaEntity> findAllByStatusAndStartAtGreaterThanEqualAndStartAtLessThan(
      AppointmentStatus status, Instant start, Instant end);
}

@Repository
class AppointmentRepositoryAdapter implements AppointmentRepository {
  private static final List<AppointmentStatus> OCCUPIED =
      List.of(
          AppointmentStatus.REQUESTED, AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS);
  private final SpringDataAppointmentRepository repository;

  AppointmentRepositoryAdapter(SpringDataAppointmentRepository repository) {
    this.repository = repository;
  }

  public Appointment save(Appointment appointment) {
    return map(repository.saveAndFlush(new AppointmentJpaEntity(appointment)));
  }

  public Optional<Appointment> findByIdAndTenantId(UUID id, Long tenantId) {
    return repository.findByIdAndTenantId(id, tenantId).map(this::map);
  }

  public Optional<Appointment> findByIdAndCustomerId(UUID id, UUID customerId) {
    return repository.findByIdAndTenantCustomerId(id, customerId).map(this::map);
  }

  public Page<Appointment> findAllByTenantId(Long tenantId, Pageable pageable) {
    return repository.findAllByTenantId(tenantId, pageable).map(this::map);
  }

  public Page<Appointment> findAllByCustomerId(UUID customerId, Pageable pageable) {
    return repository.findAllByTenantCustomerId(customerId, pageable).map(this::map);
  }

  public Page<Appointment> findAllByConsumerProfileId(UUID consumerProfileId, Pageable pageable) {
    return repository.findAllForConsumerProfile(consumerProfileId, pageable).map(this::map);
  }

  public Optional<Appointment> findByIdAndConsumerProfileId(UUID id, UUID consumerProfileId) {
    return repository.findByIdForConsumerProfile(id, consumerProfileId).map(this::map);
  }

  public boolean hasConflict(
      Long tenantId, String scheduleResource, Instant requestedStart, Instant requestedEnd) {
    return repository
        .existsByTenantIdAndScheduleResourceAndStatusInAndStartAtLessThanAndEndAtGreaterThan(
            tenantId, scheduleResource, OCCUPIED, requestedEnd, requestedStart);
  }

  public List<Appointment> findOccupiedBetween(Long tenantId, Instant start, Instant end) {
    return repository
        .findAllByTenantIdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(
            tenantId, OCCUPIED, end, start)
        .stream()
        .map(this::map)
        .toList();
  }

  public List<Appointment> findConfirmedStartingBetween(Instant start, Instant end) {
    return repository
        .findAllByStatusAndStartAtGreaterThanEqualAndStartAtLessThan(
            AppointmentStatus.CONFIRMED, start, end)
        .stream()
        .map(this::map)
        .toList();
  }

  private Appointment map(AppointmentJpaEntity e) {
    return new Appointment(
        e.id,
        e.tenantId,
        e.tenantCustomerId,
        e.tenantPetId,
        e.serviceId,
        e.employeeId,
        e.scheduleResource,
        e.requestedBy,
        e.startAt,
        e.endAt,
        e.status,
        e.notes,
        e.createdAt,
        e.updatedAt,
        e.version);
  }
}
