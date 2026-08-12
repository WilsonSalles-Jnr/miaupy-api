package com.miaupy.order.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerOrderRepository {
  CustomerOrder save(CustomerOrder order);

  Optional<CustomerOrder> findByConsumerProfileIdAndCheckoutKey(UUID consumerProfileId, UUID key);

  Optional<CustomerOrder> findByIdAndConsumerProfileId(UUID id, UUID consumerProfileId);

  Optional<CustomerOrder> findByIdAndTenantId(UUID id, Long tenantId);

  Optional<CustomerOrder> lockByIdAndTenantId(UUID id, Long tenantId);

  Page<CustomerOrder> findAllByConsumerProfileId(UUID consumerProfileId, Pageable pageable);

  Page<CustomerOrder> findAllByTenantId(Long tenantId, Pageable pageable);
}
