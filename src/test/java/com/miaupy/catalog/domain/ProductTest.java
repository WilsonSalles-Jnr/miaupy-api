package com.miaupy.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductTest {
  @Test
  void newProductStartsActiveAndUnpublished() {
    Product product =
        Product.create(101L, " sku-1 ", "Food", null, new BigDecimal("10.00"), null, 2);

    assertThat(product.sku()).isEqualTo("SKU-1");
    assertThat(product.active()).isTrue();
    assertThat(product.published()).isFalse();
  }

  @Test
  void rejectsPromotionalPriceAboveRegularPrice() {
    assertThatThrownBy(
            () ->
                Product.create(
                    101L, null, "Food", null, new BigDecimal("10.00"), new BigDecimal("11.00"), 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void deactivationAlsoUnpublishesProduct() {
    Product product =
        Product.create(101L, null, "Food", null, new BigDecimal("10.00"), null, 0)
            .publish()
            .deactivate();

    assertThat(product.active()).isFalse();
    assertThat(product.published()).isFalse();
    assertThat(product.deletedAt()).isNotNull();
  }
}
