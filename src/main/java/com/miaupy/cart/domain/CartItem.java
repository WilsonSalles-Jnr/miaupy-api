package com.miaupy.cart.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public record CartItem(UUID id, UUID productId, int quantity, BigDecimal unitPrice) {
  public CartItem {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Cart item quantity must be greater than zero");
    }
    unitPrice = unitPrice.setScale(2, RoundingMode.UNNECESSARY);
  }

  public static CartItem create(UUID productId, int quantity, BigDecimal unitPrice) {
    return new CartItem(UUID.randomUUID(), productId, quantity, unitPrice);
  }

  public CartItem withQuantity(int quantity, BigDecimal currentPrice) {
    return new CartItem(id, productId, quantity, currentPrice);
  }

  public BigDecimal total() {
    return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.UNNECESSARY);
  }
}
