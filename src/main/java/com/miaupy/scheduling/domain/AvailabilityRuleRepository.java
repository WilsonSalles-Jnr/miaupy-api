package com.miaupy.scheduling.domain;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AvailabilityRuleRepository {
  AvailabilityRule save(AvailabilityRule rule);

  Optional<AvailabilityRule> findByIdAndTenantId(UUID id, Long tenantId);

  List<AvailabilityRule> findAllByTenantId(Long tenantId);

  List<AvailabilityRule> findActiveByTenantIdAndDay(Long tenantId, DayOfWeek day);
}
