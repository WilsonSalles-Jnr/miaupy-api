package com.miaupy.cart.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CartTest {
  @Test
  void combinesRepeatedProductAndCalculatesSubtotal() {
    UUID productId = UUID.randomUUID();
    Cart cart =
        Cart.create(UUID.randomUUID(), 101L)
            .add(productId, 2, new BigDecimal("10.00"))
            .add(productId, 1, new BigDecimal("12.00"));

    assertThat(cart.items()).hasSize(1);
    assertThat(cart.items().getFirst().quantity()).isEqualTo(3);
    assertThat(cart.items().getFirst().unitPrice()).isEqualByComparingTo("12.00");
    assertThat(cart.subtotal()).isEqualByComparingTo("36.00");
  }

  @Test
  void checkedOutCartCannotBeChanged() {
    Cart cart =
        Cart.create(UUID.randomUUID(), 101L)
            .add(UUID.randomUUID(), 1, new BigDecimal("10.00"))
            .checkedOut();

    assertThatThrownBy(() -> cart.add(UUID.randomUUID(), 1, new BigDecimal("10.00")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void emptyCartCannotBeCheckedOut() {
    assertThatThrownBy(() -> Cart.create(UUID.randomUUID(), 101L).checkedOut())
        .isInstanceOf(IllegalArgumentException.class);
  }
}
