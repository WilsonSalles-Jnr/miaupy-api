package com.miaupy.order.api;

import com.miaupy.order.domain.CustomerOrder;
import com.miaupy.order.domain.OrderItem;
import com.miaupy.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    Long tenantId,
    UUID tenantCustomerId,
    OrderStatus status,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    List<ItemResponse> items,
    Instant createdAt,
    Instant updatedAt) {
  public static OrderResponse from(CustomerOrder order) {
    return new OrderResponse(
        order.id(),
        order.tenantId(),
        order.tenantCustomerId(),
        order.status(),
        order.subtotal(),
        order.discount(),
        order.total(),
        order.items().stream().map(ItemResponse::from).toList(),
        order.createdAt(),
        order.updatedAt());
  }

  public record ItemResponse(
      UUID id,
      UUID productId,
      String productName,
      int quantity,
      BigDecimal unitPrice,
      BigDecimal total) {
    static ItemResponse from(OrderItem item) {
      return new ItemResponse(
          item.id(),
          item.productId(),
          item.productName(),
          item.quantity(),
          item.unitPrice(),
          item.total());
    }
  }
}
