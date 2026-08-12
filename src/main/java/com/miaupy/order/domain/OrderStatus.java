package com.miaupy.order.domain;

public enum OrderStatus {
  CREATED,
  AWAITING_PAYMENT,
  PAID,
  PROCESSING,
  READY,
  COMPLETED,
  CANCELLED,
  REFUNDED
}
