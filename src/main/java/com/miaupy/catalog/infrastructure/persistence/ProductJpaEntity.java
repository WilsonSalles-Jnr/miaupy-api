package com.miaupy.catalog.infrastructure.persistence;

import com.miaupy.catalog.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product", schema = "catalog")
class ProductJpaEntity {
  @Id UUID id;

  @Column(name = "tenant_id", nullable = false)
  Long tenantId;

  @Column(length = 80)
  String sku;

  @Column(nullable = false, length = 180)
  String name;

  @Column(length = 3000)
  String description;

  @Column(nullable = false, precision = 19, scale = 2)
  BigDecimal price;

  @Column(name = "promotional_price", precision = 19, scale = 2)
  BigDecimal promotionalPrice;

  @Column(name = "stock_quantity", nullable = false)
  int stockQuantity;

  @Column(nullable = false)
  boolean active;

  @Column(nullable = false)
  boolean published;

  @Column(name = "deleted_at")
  Instant deletedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  Instant updatedAt;

  @Version Long version;

  protected ProductJpaEntity() {}

  ProductJpaEntity(Product p) {
    id = p.id();
    tenantId = p.tenantId();
    sku = p.sku();
    name = p.name();
    description = p.description();
    price = p.price();
    promotionalPrice = p.promotionalPrice();
    stockQuantity = p.stockQuantity();
    active = p.active();
    published = p.published();
    deletedAt = p.deletedAt();
    createdAt = p.createdAt();
    updatedAt = p.updatedAt();
    version = p.version();
  }
}
