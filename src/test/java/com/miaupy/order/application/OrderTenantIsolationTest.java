package com.miaupy.order.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miaupy.catalog.domain.ProductRepository;
import com.miaupy.consumer.application.ConsumerProfileUseCase;
import com.miaupy.integration.outbox.OutboxWriter;
import com.miaupy.order.domain.CustomerOrderRepository;
import com.miaupy.shared.exception.ResourceNotFoundException;
import com.miaupy.shared.tenant.TenantContext;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderTenantIsolationTest {
  @Test
  void tenantBDoesNotFindOrderFromTenantA() {
    UUID orderId = UUID.randomUUID();
    TenantContext tenants = mock(TenantContext.class);
    CustomerOrderRepository orders = mock(CustomerOrderRepository.class);
    when(tenants.getRequiredTenantId()).thenReturn(202L);
    when(orders.findByIdAndTenantId(orderId, 202L)).thenReturn(Optional.empty());
    OrderUseCase useCase =
        new OrderUseCase(
            tenants,
            mock(ConsumerProfileUseCase.class),
            orders,
            mock(ProductRepository.class),
            mock(OutboxWriter.class));

    assertThatThrownBy(() -> useCase.getBusiness(orderId))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(orders).findByIdAndTenantId(orderId, 202L);
  }
}
