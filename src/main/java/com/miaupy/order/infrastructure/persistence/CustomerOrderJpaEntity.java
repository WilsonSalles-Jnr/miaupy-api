package com.miaupy.order.infrastructure.persistence;

import com.miaupy.order.domain.CustomerOrder;
import com.miaupy.order.domain.OrderItem;
import com.miaupy.order.domain.OrderStatus;
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
@Table(name = "customer_order", schema = "sales")
class CustomerOrderJpaEntity {
  @Id UUID id;

  @Column(name = "tenant_id", nullable = false)
  Long tenantId;

  @Column(name = "consumer_profile_id", nullable = false)
  UUID consumerProfileId;

  @Column(name = "tenant_customer_id")
  UUID tenantCustomerId;

  @Column(name = "checkout_key", nullable = false)
  UUID checkoutKey;

  @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
  @Column(nullable = false, length = 30)
  OrderStatus status;

  @Column(nullable = false, precision = 19, scale = 2)
  BigDecimal subtotal;

  @Column(nullable = false, precision = 19, scale = 2)
  BigDecimal discount;

  @Column(nullable = false, precision = 19, scale = 2)
  BigDecimal total;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  List<OrderItemJpaEntity> items = new ArrayList<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  Instant updatedAt;

  @Version Long version;

  protected CustomerOrderJpaEntity() {}

  CustomerOrderJpaEntity(CustomerOrder order) {
    id = order.id();
    tenantId = order.tenantId();
    consumerProfileId = order.consumerProfileId();
    tenantCustomerId = order.tenantCustomerId();
    checkoutKey = order.checkoutKey();
    status = order.status();
    subtotal = order.subtotal();
    discount = order.discount();
    total = order.total();
    items = order.items().stream().map(item -> new OrderItemJpaEntity(item, tenantId)).toList();
    createdAt = order.createdAt();
    updatedAt = order.updatedAt();
    version = order.version();
  }

  CustomerOrder toDomain() {
    return new CustomerOrder(
        id,
        tenantId,
        consumerProfileId,
        tenantCustomerId,
        checkoutKey,
        status,
        subtotal,
        discount,
        total,
        items.stream().map(OrderItemJpaEntity::toDomain).toList(),
        createdAt,
        updatedAt,
        version);
  }
}

@Entity
@Table(name = "order_item", schema = "sales")
class OrderItemJpaEntity {
  @Id UUID id;

  @Column(name = "tenant_id", nullable = false)
  Long tenantId;

  @Column(name = "product_id", nullable = false)
  UUID productId;

  @Column(name = "product_name", nullable = false, length = 180)
  String productName;

  @Column(nullable = false)
  int quantity;

  @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
  BigDecimal unitPrice;

  @Column(nullable = false, precision = 19, scale = 2)
  BigDecimal total;

  protected OrderItemJpaEntity() {}

  OrderItemJpaEntity(OrderItem item, Long tenantId) {
    id = item.id();
    this.tenantId = tenantId;
    productId = item.productId();
    productName = item.productName();
    quantity = item.quantity();
    unitPrice = item.unitPrice();
    total = item.total();
  }

  OrderItem toDomain() {
    return new OrderItem(id, productId, productName, quantity, unitPrice, total);
  }
}
