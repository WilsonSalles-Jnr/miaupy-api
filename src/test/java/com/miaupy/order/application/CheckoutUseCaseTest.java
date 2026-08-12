package com.miaupy.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miaupy.business.domain.AppointmentApprovalMode;
import com.miaupy.business.domain.BusinessConfigurationRepository;
import com.miaupy.business.domain.BusinessSettings;
import com.miaupy.cart.domain.Cart;
import com.miaupy.cart.domain.CartRepository;
import com.miaupy.catalog.domain.Product;
import com.miaupy.catalog.domain.ProductRepository;
import com.miaupy.consumer.application.ConsumerProfileUseCase;
import com.miaupy.consumer.domain.ConsumerProfile;
import com.miaupy.customer.domain.TenantCustomerRepository;
import com.miaupy.integration.outbox.OutboxWriter;
import com.miaupy.order.domain.CustomerOrder;
import com.miaupy.order.domain.CustomerOrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CheckoutUseCaseTest {
  @Test
  void retryWithSameKeyReturnsExistingOrderWithoutReservingStockAgain() {
    Fixtures f = new Fixtures();
    CustomerOrder existing = f.order();
    when(f.orders.findByConsumerProfileIdAndCheckoutKey(f.profile.id(), f.key))
        .thenReturn(Optional.of(existing));

    CustomerOrder result = f.useCase().checkout(f.key);

    assertThat(result).isSameAs(existing);
    verify(f.carts, never()).lockActiveByConsumerProfileId(any());
    verify(f.products, never()).save(any());
  }

  @Test
  void checkoutSnapshotsCurrentPriceReservesStockAndWritesOutbox() {
    Fixtures f = new Fixtures();
    Product product =
        Product.create(
                f.tenant,
                "FOOD",
                "Premium food",
                null,
                new BigDecimal("30.00"),
                new BigDecimal("25.00"),
                5)
            .publish();
    Cart cart = Cart.create(f.profile.id(), f.tenant).add(product.id(), 2, product.sellingPrice());
    when(f.orders.findByConsumerProfileIdAndCheckoutKey(f.profile.id(), f.key))
        .thenReturn(Optional.empty());
    when(f.carts.lockActiveByConsumerProfileId(f.profile.id())).thenReturn(Optional.of(cart));
    when(f.configurations.findSettingsByTenantId(f.tenant))
        .thenReturn(
            Optional.of(
                BusinessSettings.create(
                    f.tenant,
                    AppointmentApprovalMode.MANUAL,
                    "America/Sao_Paulo",
                    "BRL",
                    true,
                    true)));
    when(f.products.lockByIdAndTenantId(product.id(), f.tenant)).thenReturn(Optional.of(product));
    when(f.products.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(f.customers.findByConsumerProfileIdAndTenantId(f.profile.id(), f.tenant))
        .thenReturn(Optional.empty());
    when(f.orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(f.carts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    CustomerOrder order = f.useCase().checkout(f.key);

    assertThat(order.total()).isEqualByComparingTo("50.00");
    assertThat(order.items().getFirst().productName()).isEqualTo("Premium food");
    assertThat(order.items().getFirst().unitPrice()).isEqualByComparingTo("25.00");
    verify(f.products)
        .save(org.mockito.ArgumentMatchers.argThat(saved -> saved.stockQuantity() == 3));
    verify(f.outbox, org.mockito.Mockito.times(2)).append(any(), any(), any(), any(), any());
  }

  private static final class Fixtures {
    private final Long tenant = 101L;
    private final UUID key = UUID.randomUUID();
    private final ConsumerProfile profile =
        new ConsumerProfile(
            UUID.randomUUID(),
            "subject",
            "Consumer",
            "consumer@example.com",
            null,
            null,
            null,
            true,
            Instant.now(),
            Instant.now(),
            0L);
    private final ConsumerProfileUseCase profiles = mock(ConsumerProfileUseCase.class);
    private final CartRepository carts = mock(CartRepository.class);
    private final CustomerOrderRepository orders = mock(CustomerOrderRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final TenantCustomerRepository customers = mock(TenantCustomerRepository.class);
    private final BusinessConfigurationRepository configurations =
        mock(BusinessConfigurationRepository.class);
    private final OutboxWriter outbox = mock(OutboxWriter.class);

    private Fixtures() {
      when(profiles.getMe()).thenReturn(profile);
    }

    private CheckoutUseCase useCase() {
      return new CheckoutUseCase(
          profiles, carts, orders, products, customers, configurations, outbox);
    }

    private CustomerOrder order() {
      return CustomerOrder.create(
          tenant,
          profile.id(),
          null,
          key,
          List.of(
              com.miaupy.order.domain.OrderItem.snapshot(
                  UUID.randomUUID(), "Food", 1, new BigDecimal("10.00"))));
    }
  }
}
