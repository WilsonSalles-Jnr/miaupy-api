package com.miaupy.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerOrderTest {
  @Test
  void storesProductAndPriceSnapshot() {
    OrderItem item =
        OrderItem.snapshot(UUID.randomUUID(), "Premium food", 2, new BigDecimal("25.50"));
    CustomerOrder order =
        CustomerOrder.create(101L, UUID.randomUUID(), null, UUID.randomUUID(), List.of(item));

    assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
    assertThat(order.total()).isEqualByComparingTo("51.00");
    assertThat(order.items().getFirst().productName()).isEqualTo("Premium food");
    assertThat(order.items().getFirst().unitPrice()).isEqualByComparingTo("25.50");
  }

  @Test
  void enforcesOrderStateMachine() {
    CustomerOrder order = order().transitionTo(OrderStatus.PROCESSING);
    assertThat(order.transitionTo(OrderStatus.READY).transitionTo(OrderStatus.COMPLETED).status())
        .isEqualTo(OrderStatus.COMPLETED);
    assertThatThrownBy(() -> order.transitionTo(OrderStatus.COMPLETED))
        .isInstanceOf(InvalidOrderTransitionException.class);
  }

  private CustomerOrder order() {
    return CustomerOrder.create(
        101L,
        UUID.randomUUID(),
        null,
        UUID.randomUUID(),
        List.of(OrderItem.snapshot(UUID.randomUUID(), "Food", 1, new BigDecimal("10.00"))));
  }
}
