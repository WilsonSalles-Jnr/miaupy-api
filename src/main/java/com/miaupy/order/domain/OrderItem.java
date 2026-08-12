package com.miaupy.order.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public record OrderItem(
    UUID id,
    UUID productId,
    String productName,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal total) {
  public static OrderItem snapshot(
      UUID productId, String productName, int quantity, BigDecimal unitPrice) {
    BigDecimal normalized = unitPrice.setScale(2, RoundingMode.UNNECESSARY);
    return new OrderItem(
        UUID.randomUUID(),
        productId,
        productName,
        quantity,
        normalized,
        normalized.multiply(BigDecimal.valueOf(quantity)).setScale(2));
  }
}
