package com.miaupy.catalog.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OfferedServiceRepository {
  OfferedService save(OfferedService service);

  Optional<OfferedService> findByIdAndTenantId(UUID id, Long tenantId);

  Page<OfferedService> findAllByTenantId(Long tenantId, Pageable pageable);

  Page<OfferedService> findPublishedByTenantId(Long tenantId, Pageable pageable);

  Optional<OfferedService> findPublishedByIdAndTenantId(UUID id, Long tenantId);
}
