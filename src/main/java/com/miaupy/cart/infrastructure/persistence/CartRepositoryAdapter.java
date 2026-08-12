package com.miaupy.cart.infrastructure.persistence;

import com.miaupy.cart.domain.Cart;
import com.miaupy.cart.domain.CartRepository;
import com.miaupy.cart.domain.CartStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

interface SpringDataCartRepository extends JpaRepository<CartJpaEntity, UUID> {
  @EntityGraph(attributePaths = "items")
  Optional<CartJpaEntity> findByConsumerProfileIdAndStatus(
      UUID consumerProfileId, CartStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @EntityGraph(attributePaths = "items")
  Optional<CartJpaEntity> findLockedByConsumerProfileIdAndStatus(
      UUID consumerProfileId, CartStatus status);
}

@Repository
class CartRepositoryAdapter implements CartRepository {
  private final SpringDataCartRepository repository;

  CartRepositoryAdapter(SpringDataCartRepository repository) {
    this.repository = repository;
  }

  public Cart save(Cart cart) {
    return repository.save(new CartJpaEntity(cart)).toDomain();
  }

  public Optional<Cart> findActiveByConsumerProfileId(UUID consumerProfileId) {
    return repository
        .findByConsumerProfileIdAndStatus(consumerProfileId, CartStatus.ACTIVE)
        .map(CartJpaEntity::toDomain);
  }

  public Optional<Cart> lockActiveByConsumerProfileId(UUID consumerProfileId) {
    return repository
        .findLockedByConsumerProfileIdAndStatus(consumerProfileId, CartStatus.ACTIVE)
        .map(CartJpaEntity::toDomain);
  }
}
