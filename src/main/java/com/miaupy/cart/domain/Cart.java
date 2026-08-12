package com.miaupy.cart.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record Cart(
    UUID id,
    UUID consumerProfileId,
    Long tenantId,
    CartStatus status,
    List<CartItem> items,
    Instant createdAt,
    Instant updatedAt,
    Long version) {
  public Cart {
    items = List.copyOf(items);
  }

  public static Cart create(UUID consumerProfileId, Long tenantId) {
    Instant now = Instant.now();
    return new Cart(
        UUID.randomUUID(),
        consumerProfileId,
        tenantId,
        CartStatus.ACTIVE,
        List.of(),
        now,
        now,
        null);
  }

  public Cart add(UUID productId, int quantity, BigDecimal unitPrice) {
    requireActive();
    List<CartItem> changed = new ArrayList<>(items);
    int index = indexOfProduct(productId);
    if (index >= 0) {
      CartItem current = changed.get(index);
      changed.set(
          index, current.withQuantity(Math.addExact(current.quantity(), quantity), unitPrice));
    } else {
      changed.add(CartItem.create(productId, quantity, unitPrice));
    }
    return copy(CartStatus.ACTIVE, changed);
  }

  public Cart updateItem(UUID itemId, int quantity, BigDecimal currentPrice) {
    requireActive();
    List<CartItem> changed = new ArrayList<>(items);
    int index = indexOfItem(itemId);
    if (index < 0) {
      throw new IllegalArgumentException("Cart item not found");
    }
    changed.set(index, changed.get(index).withQuantity(quantity, currentPrice));
    return copy(CartStatus.ACTIVE, changed);
  }

  public Cart removeItem(UUID itemId) {
    requireActive();
    List<CartItem> changed = new ArrayList<>(items);
    if (!changed.removeIf(item -> item.id().equals(itemId))) {
      throw new IllegalArgumentException("Cart item not found");
    }
    return copy(CartStatus.ACTIVE, changed);
  }

  public Cart checkedOut() {
    requireActive();
    if (items.isEmpty()) {
      throw new IllegalArgumentException("Cart is empty");
    }
    return copy(CartStatus.CHECKED_OUT, items);
  }

  public BigDecimal subtotal() {
    return items.stream().map(CartItem::total).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
  }

  private void requireActive() {
    if (status != CartStatus.ACTIVE) {
      throw new IllegalArgumentException("Cart is not active");
    }
  }

  private int indexOfProduct(UUID productId) {
    for (int index = 0; index < items.size(); index++) {
      if (items.get(index).productId().equals(productId)) return index;
    }
    return -1;
  }

  private int indexOfItem(UUID itemId) {
    for (int index = 0; index < items.size(); index++) {
      if (items.get(index).id().equals(itemId)) return index;
    }
    return -1;
  }

  private Cart copy(CartStatus newStatus, List<CartItem> newItems) {
    return new Cart(
        id, consumerProfileId, tenantId, newStatus, newItems, createdAt, Instant.now(), version);
  }
}
