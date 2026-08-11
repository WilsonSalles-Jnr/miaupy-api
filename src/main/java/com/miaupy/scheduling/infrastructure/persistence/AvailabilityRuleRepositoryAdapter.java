package com.miaupy.scheduling.infrastructure.persistence;

import com.miaupy.scheduling.domain.AvailabilityRule;
import com.miaupy.scheduling.domain.AvailabilityRuleRepository;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Entity
@Table(name = "availability_rule", schema = "scheduling")
class AvailabilityRuleJpaEntity {
  @Id UUID id;

  @Column(name = "tenant_id", nullable = false)
  Long tenantId;

  @Column(name = "employee_id")
  UUID employeeId;

  @Column(name = "day_of_week", nullable = false)
  Short dayOfWeek;

  @Column(name = "start_local", nullable = false)
  LocalTime startLocal;

  @Column(name = "end_local", nullable = false)
  LocalTime endLocal;

  @Column(nullable = false)
  boolean active;

  @Column(name = "created_at", nullable = false)
  Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  Instant updatedAt;

  protected AvailabilityRuleJpaEntity() {}

  AvailabilityRuleJpaEntity(AvailabilityRule r) {
    id = r.id();
    tenantId = r.tenantId();
    employeeId = r.employeeId();
    dayOfWeek = (short) r.dayOfWeek().getValue();
    startLocal = r.startLocal();
    endLocal = r.endLocal();
    active = r.active();
    createdAt = r.createdAt();
    updatedAt = r.updatedAt();
  }
}

interface SpringDataAvailabilityRuleRepository
    extends JpaRepository<AvailabilityRuleJpaEntity, UUID> {
  Optional<AvailabilityRuleJpaEntity> findByIdAndTenantIdAndActiveTrue(UUID id, Long tenantId);

  List<AvailabilityRuleJpaEntity> findAllByTenantIdAndActiveTrueOrderByDayOfWeekAscStartLocalAsc(
      Long tenantId);

  List<AvailabilityRuleJpaEntity> findAllByTenantIdAndDayOfWeekAndActiveTrueOrderByStartLocalAsc(
      Long tenantId, Short day);
}

@Repository
class AvailabilityRuleRepositoryAdapter implements AvailabilityRuleRepository {
  private final SpringDataAvailabilityRuleRepository repository;

  AvailabilityRuleRepositoryAdapter(SpringDataAvailabilityRuleRepository repository) {
    this.repository = repository;
  }

  public AvailabilityRule save(AvailabilityRule rule) {
    return map(repository.save(new AvailabilityRuleJpaEntity(rule)));
  }

  public Optional<AvailabilityRule> findByIdAndTenantId(UUID id, Long tenantId) {
    return repository.findByIdAndTenantIdAndActiveTrue(id, tenantId).map(this::map);
  }

  public List<AvailabilityRule> findAllByTenantId(Long tenantId) {
    return repository
        .findAllByTenantIdAndActiveTrueOrderByDayOfWeekAscStartLocalAsc(tenantId)
        .stream()
        .map(this::map)
        .toList();
  }

  public List<AvailabilityRule> findActiveByTenantIdAndDay(Long tenantId, DayOfWeek day) {
    return repository
        .findAllByTenantIdAndDayOfWeekAndActiveTrueOrderByStartLocalAsc(
            tenantId, (short) day.getValue())
        .stream()
        .map(this::map)
        .toList();
  }

  private AvailabilityRule map(AvailabilityRuleJpaEntity e) {
    return new AvailabilityRule(
        e.id,
        e.tenantId,
        e.employeeId,
        DayOfWeek.of(e.dayOfWeek),
        e.startLocal,
        e.endLocal,
        e.active,
        e.createdAt,
        e.updatedAt);
  }
}
