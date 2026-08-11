package com.miaupy.catalog.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

public record Product(
    UUID id,
    Long tenantId,
    String sku,
    String name,
    String description,
    BigDecimal price,
    BigDecimal promotionalPrice,
    int stockQuantity,
    boolean active,
    boolean published,
    Instant deletedAt,
    Instant createdAt,
    Instant updatedAt,
    Long version) {

  public static Product create(
      Long tenantId,
      String sku,
      String name,
      String description,
      BigDecimal price,
      BigDecimal promotionalPrice,
      int stockQuantity) {
    Instant now = Instant.now();
    return new Product(
        UUID.randomUUID(),
        tenantId,
        normalizeSku(sku),
        name,
        description,
        money(price),
        promotionalMoney(promotionalPrice, price),
        stockQuantity,
        true,
        false,
        null,
        now,
        now,
        null);
  }

  public Product update(
      String sku,
      String name,
      String description,
      BigDecimal price,
      BigDecimal promotionalPrice,
      int stockQuantity) {
    return new Product(
        id,
        tenantId,
        normalizeSku(sku),
        name,
        description,
        money(price),
        promotionalMoney(promotionalPrice, price),
        stockQuantity,
        active,
        published,
        deletedAt,
        createdAt,
        Instant.now(),
        version);
  }

  public Product publish() {
    if (!active) {
      throw new IllegalArgumentException("Inactive product cannot be published");
    }
    return copy(true, active, deletedAt);
  }

  public Product unpublish() {
    return copy(false, active, deletedAt);
  }

  public Product deactivate() {
    return copy(false, false, Instant.now());
  }

  private Product copy(boolean published, boolean active, Instant deletedAt) {
    return new Product(
        id,
        tenantId,
        sku,
        name,
        description,
        price,
        promotionalPrice,
        stockQuantity,
        active,
        published,
        deletedAt,
        createdAt,
        Instant.now(),
        version);
  }

  private static BigDecimal money(BigDecimal value) {
    if (value == null || value.signum() <= 0) {
      throw new IllegalArgumentException("Product price must be greater than zero");
    }
    return value.setScale(2, RoundingMode.UNNECESSARY);
  }

  private static BigDecimal promotionalMoney(BigDecimal promotional, BigDecimal regular) {
    if (promotional == null) {
      return null;
    }
    BigDecimal normalized = money(promotional);
    if (normalized.compareTo(money(regular)) > 0) {
      throw new IllegalArgumentException("Promotional price cannot exceed regular price");
    }
    return normalized;
  }

  private static String normalizeSku(String sku) {
    return sku == null || sku.isBlank() ? null : sku.strip().toUpperCase();
  }
}
