package com.miaupy.pet.infrastructure.persistence;

import com.miaupy.pet.domain.*;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
class TenantPetRepositoryAdapter implements TenantPetRepository {
  private final SpringDataTenantPetRepository repository;

  TenantPetRepositoryAdapter(SpringDataTenantPetRepository repository) {
    this.repository = repository;
  }

  public TenantPet save(TenantPet p) {
    return map(repository.save(new TenantPetJpaEntity(p)));
  }

  public Optional<TenantPet> findByIdAndTenantId(UUID id, Long tenant) {
    return repository.findByIdAndTenantIdAndActiveTrue(id, tenant).map(this::map);
  }

  public Optional<TenantPet> findByConsumerPetIdAndTenantId(UUID consumerPetId, Long tenantId) {
    return repository
        .findByConsumerPetIdAndTenantIdAndActiveTrue(consumerPetId, tenantId)
        .map(this::map);
  }

  public Page<TenantPet> findAllByCustomerIdAndTenantId(
      UUID customer, Long tenant, Pageable pageable) {
    return repository
        .findAllByTenantCustomerIdAndTenantIdAndActiveTrue(customer, tenant, pageable)
        .map(this::map);
  }

  private TenantPet map(TenantPetJpaEntity e) {
    return new TenantPet(
        e.id,
        e.tenantId,
        e.tenantCustomerId,
        e.consumerPetId,
        e.name,
        e.species,
        e.breed,
        e.birthDate,
        e.sex,
        e.weight,
        e.notes,
        e.active,
        e.deletedAt,
        e.createdAt,
        e.updatedAt,
        e.version);
  }
}
