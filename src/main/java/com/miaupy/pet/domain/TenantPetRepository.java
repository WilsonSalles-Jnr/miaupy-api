package com.miaupy.pet.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TenantPetRepository {
  TenantPet save(TenantPet pet);

  Optional<TenantPet> findByIdAndTenantId(UUID id, Long tenantId);

  Optional<TenantPet> findByConsumerPetIdAndTenantId(UUID consumerPetId, Long tenantId);

  Page<TenantPet> findAllByCustomerIdAndTenantId(UUID customerId, Long tenantId, Pageable pageable);
}
