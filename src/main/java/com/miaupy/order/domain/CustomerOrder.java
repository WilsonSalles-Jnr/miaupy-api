package com.miaupy.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public record CustomerOrder(
    UUID id,
    Long tenantId,
    UUID consumerProfileId,
    UUID tenantCustomerId,
    UUID checkoutKey,
    OrderStatus status,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    List<OrderItem> items,
    Instant createdAt,
    Instant updatedAt,
    Long version) {
  public CustomerOrder {
    items = List.copyOf(items);
  }

  public static CustomerOrder create(
      Long tenantId,
      UUID consumerProfileId,
      UUID tenantCustomerId,
      UUID checkoutKey,
      List<OrderItem> items) {
    if (items.isEmpty()) throw new IllegalArgumentException("Order must contain at least one item");
    BigDecimal subtotal =
        items.stream().map(OrderItem::total).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
    Instant now = Instant.now();
    return new CustomerOrder(
        UUID.randomUUID(),
        tenantId,
        consumerProfileId,
        tenantCustomerId,
        checkoutKey,
        OrderStatus.CREATED,
        subtotal,
        BigDecimal.ZERO.setScale(2),
        subtotal,
        items,
        now,
        now,
        null);
  }

  public CustomerOrder transitionTo(OrderStatus target) {
    boolean valid =
        switch (status) {
          case CREATED ->
              EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED).contains(target);
          case AWAITING_PAYMENT ->
              EnumSet.of(OrderStatus.PAID, OrderStatus.CANCELLED).contains(target);
          case PAID -> EnumSet.of(OrderStatus.PROCESSING, OrderStatus.REFUNDED).contains(target);
          case PROCESSING -> EnumSet.of(OrderStatus.READY, OrderStatus.CANCELLED).contains(target);
          case READY -> EnumSet.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED).contains(target);
          case COMPLETED, CANCELLED, REFUNDED -> false;
        };
    if (!valid) {
      throw new InvalidOrderTransitionException(status, target);
    }
    return new CustomerOrder(
        id,
        tenantId,
        consumerProfileId,
        tenantCustomerId,
        checkoutKey,
        target,
        subtotal,
        discount,
        total,
        items,
        createdAt,
        Instant.now(),
        version);
  }
}
