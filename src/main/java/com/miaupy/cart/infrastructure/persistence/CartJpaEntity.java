package com.miaupy.cart.infrastructure.persistence;

import com.miaupy.cart.domain.Cart;
import com.miaupy.cart.domain.CartItem;
import com.miaupy.cart.domain.CartStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cart", schema = "sales")
class CartJpaEntity {
  @Id UUID id;

  @Column(name = "consumer_profile_id", nullable = false)
  UUID consumerProfileId;

  @Column(name = "tenant_id", nullable = false)
  Long tenantId;

  @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
  @Column(nullable = false, length = 20)
  CartStatus status;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "cart_id", nullable = false)
  List<CartItemJpaEntity> items = new ArrayList<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  Instant updatedAt;

  @Version Long version;

  protected CartJpaEntity() {}

  CartJpaEntity(Cart cart) {
    id = cart.id();
    consumerProfileId = cart.consumerProfileId();
    tenantId = cart.tenantId();
    status = cart.status();
    items = cart.items().stream().map(item -> new CartItemJpaEntity(item, tenantId)).toList();
    createdAt = cart.createdAt();
    updatedAt = cart.updatedAt();
    version = cart.version();
  }

  Cart toDomain() {
    return new Cart(
        id,
        consumerProfileId,
        tenantId,
        status,
        items.stream().map(CartItemJpaEntity::toDomain).toList(),
        createdAt,
        updatedAt,
        version);
  }
}

@Entity
@Table(name = "cart_item", schema = "sales")
class CartItemJpaEntity {
  @Id UUID id;

  @Column(name = "tenant_id", nullable = false)
  Long tenantId;

  @Column(name = "product_id", nullable = false)
  UUID productId;

  @Column(nullable = false)
  int quantity;

  @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
  BigDecimal unitPrice;

  protected CartItemJpaEntity() {}

  CartItemJpaEntity(CartItem item, Long tenantId) {
    id = item.id();
    this.tenantId = tenantId;
    productId = item.productId();
    quantity = item.quantity();
    unitPrice = item.unitPrice();
  }

  CartItem toDomain() {
    return new CartItem(id, productId, quantity, unitPrice);
  }
}
