package com.miaupy.customer.infrastructure.persistence;

import com.miaupy.customer.domain.*;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
class TenantCustomerRepositoryAdapter implements TenantCustomerRepository {
  private final SpringDataTenantCustomerRepository repository;

  TenantCustomerRepositoryAdapter(SpringDataTenantCustomerRepository repository) {
    this.repository = repository;
  }

  public TenantCustomer save(TenantCustomer c) {
    return map(repository.save(new TenantCustomerJpaEntity(c)));
  }

  public Optional<TenantCustomer> findByIdAndTenantId(UUID id, Long tenantId) {
    return repository.findByIdAndTenantIdAndActiveTrue(id, tenantId).map(this::map);
  }

  public Page<TenantCustomer> findAllByTenantId(Long tenantId, Pageable p) {
    return repository.findAllByTenantIdAndActiveTrue(tenantId, p).map(this::map);
  }

  private TenantCustomer map(TenantCustomerJpaEntity e) {
    return new TenantCustomer(
        e.id,
        e.tenantId,
        e.consumerProfileId,
        e.name,
        e.email,
        e.phone,
        e.document,
        e.notes,
        e.active,
        e.deletedAt,
        e.createdAt,
        e.updatedAt,
        e.version);
  }
}
