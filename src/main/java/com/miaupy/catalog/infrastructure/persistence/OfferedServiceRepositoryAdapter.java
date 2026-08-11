package com.miaupy.catalog.infrastructure.persistence;

import com.miaupy.catalog.domain.OfferedService;
import com.miaupy.catalog.domain.OfferedServiceRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

interface SpringDataOfferedServiceRepository extends JpaRepository<OfferedServiceJpaEntity, UUID> {
  Optional<OfferedServiceJpaEntity> findByIdAndTenantIdAndActiveTrue(UUID id, Long tenantId);

  Page<OfferedServiceJpaEntity> findAllByTenantIdAndActiveTrue(Long tenantId, Pageable pageable);

  Page<OfferedServiceJpaEntity> findAllByTenantIdAndActiveTrueAndPublishedTrue(
      Long tenantId, Pageable pageable);

  Optional<OfferedServiceJpaEntity> findByIdAndTenantIdAndActiveTrueAndPublishedTrue(
      UUID id, Long tenantId);
}

@Repository
class OfferedServiceRepositoryAdapter implements OfferedServiceRepository {
  private final SpringDataOfferedServiceRepository repository;

  OfferedServiceRepositoryAdapter(SpringDataOfferedServiceRepository repository) {
    this.repository = repository;
  }

  public OfferedService save(OfferedService service) {
    return map(repository.save(new OfferedServiceJpaEntity(service)));
  }

  public Optional<OfferedService> findByIdAndTenantId(UUID id, Long tenantId) {
    return repository.findByIdAndTenantIdAndActiveTrue(id, tenantId).map(this::map);
  }

  public Page<OfferedService> findAllByTenantId(Long tenantId, Pageable pageable) {
    return repository.findAllByTenantIdAndActiveTrue(tenantId, pageable).map(this::map);
  }

  public Page<OfferedService> findPublishedByTenantId(Long tenantId, Pageable pageable) {
    return repository
        .findAllByTenantIdAndActiveTrueAndPublishedTrue(tenantId, pageable)
        .map(this::map);
  }

  public Optional<OfferedService> findPublishedByIdAndTenantId(UUID id, Long tenantId) {
    return repository.findByIdAndTenantIdAndActiveTrueAndPublishedTrue(id, tenantId).map(this::map);
  }

  private OfferedService map(OfferedServiceJpaEntity e) {
    return new OfferedService(
        e.id,
        e.tenantId,
        e.name,
        e.description,
        e.durationMinutes,
        e.price,
        e.active,
        e.published,
        e.requiresApproval,
        e.deletedAt,
        e.createdAt,
        e.updatedAt,
        e.version);
  }
}
