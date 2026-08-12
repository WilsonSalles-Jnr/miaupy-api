package com.miaupy.order.domain;

public class InvalidOrderTransitionException extends RuntimeException {
  public InvalidOrderTransitionException(OrderStatus current, OrderStatus target) {
    super("Invalid order transition from " + current + " to " + target);
  }
}
