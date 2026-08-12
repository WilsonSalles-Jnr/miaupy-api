package com.miaupy.order.infrastructure.persistence;

import com.miaupy.order.domain.CustomerOrder;
import com.miaupy.order.domain.CustomerOrderRepository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

interface SpringDataCustomerOrderRepository extends JpaRepository<CustomerOrderJpaEntity, UUID> {
  @EntityGraph(attributePaths = "items")
  Optional<CustomerOrderJpaEntity> findByConsumerProfileIdAndCheckoutKey(
      UUID consumerProfileId, UUID checkoutKey);

  @EntityGraph(attributePaths = "items")
  Optional<CustomerOrderJpaEntity> findByIdAndConsumerProfileId(UUID id, UUID consumerProfileId);

  @EntityGraph(attributePaths = "items")
  Optional<CustomerOrderJpaEntity> findByIdAndTenantId(UUID id, Long tenantId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @EntityGraph(attributePaths = "items")
  Optional<CustomerOrderJpaEntity> findLockedByIdAndTenantId(UUID id, Long tenantId);

  @EntityGraph(attributePaths = "items")
  Page<CustomerOrderJpaEntity> findAllByConsumerProfileId(
      UUID consumerProfileId, Pageable pageable);

  @EntityGraph(attributePaths = "items")
  Page<CustomerOrderJpaEntity> findAllByTenantId(Long tenantId, Pageable pageable);
}

@Repository
class CustomerOrderRepositoryAdapter implements CustomerOrderRepository {
  private final SpringDataCustomerOrderRepository repository;

  CustomerOrderRepositoryAdapter(SpringDataCustomerOrderRepository repository) {
    this.repository = repository;
  }

  public CustomerOrder save(CustomerOrder order) {
    return repository.save(new CustomerOrderJpaEntity(order)).toDomain();
  }

  public Optional<CustomerOrder> findByConsumerProfileIdAndCheckoutKey(
      UUID consumerProfileId, UUID key) {
    return repository
        .findByConsumerProfileIdAndCheckoutKey(consumerProfileId, key)
        .map(CustomerOrderJpaEntity::toDomain);
  }

  public Optional<CustomerOrder> findByIdAndConsumerProfileId(UUID id, UUID consumerProfileId) {
    return repository
        .findByIdAndConsumerProfileId(id, consumerProfileId)
        .map(CustomerOrderJpaEntity::toDomain);
  }

  public Optional<CustomerOrder> findByIdAndTenantId(UUID id, Long tenantId) {
    return repository.findByIdAndTenantId(id, tenantId).map(CustomerOrderJpaEntity::toDomain);
  }

  public Optional<CustomerOrder> lockByIdAndTenantId(UUID id, Long tenantId) {
    return repository.findLockedByIdAndTenantId(id, tenantId).map(CustomerOrderJpaEntity::toDomain);
  }

  public Page<CustomerOrder> findAllByConsumerProfileId(UUID consumerProfileId, Pageable pageable) {
    return repository
        .findAllByConsumerProfileId(consumerProfileId, pageable)
        .map(CustomerOrderJpaEntity::toDomain);
  }

  public Page<CustomerOrder> findAllByTenantId(Long tenantId, Pageable pageable) {
    return repository.findAllByTenantId(tenantId, pageable).map(CustomerOrderJpaEntity::toDomain);
  }
}
