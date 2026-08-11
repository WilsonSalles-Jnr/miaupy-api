package com.miaupy.catalog.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miaupy.catalog.domain.ProductRepository;
import com.miaupy.integration.outbox.OutboxWriter;
import com.miaupy.shared.exception.ResourceNotFoundException;
import com.miaupy.shared.tenant.TenantContext;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductTenantIsolationTest {
  @Test
  void tenantBDoesNotFindProductFromTenantA() {
    UUID productId = UUID.randomUUID();
    TenantContext tenants = mock(TenantContext.class);
    ProductRepository repository = mock(ProductRepository.class);
    when(tenants.getRequiredTenantId()).thenReturn(202L);
    when(repository.findByIdAndTenantId(productId, 202L)).thenReturn(Optional.empty());
    ProductUseCase useCase =
        new ProductUseCase(
            tenants, repository, mock(OutboxWriter.class), mock(CatalogCacheInvalidator.class));

    assertThatThrownBy(() -> useCase.get(productId)).isInstanceOf(ResourceNotFoundException.class);
    verify(repository).findByIdAndTenantId(productId, 202L);
  }
}
