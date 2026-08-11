package com.miaupy.catalog.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miaupy.catalog.domain.OfferedServiceRepository;
import com.miaupy.integration.outbox.OutboxWriter;
import com.miaupy.shared.exception.ResourceNotFoundException;
import com.miaupy.shared.tenant.TenantContext;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OfferedServiceTenantIsolationTest {
  @Test
  void tenantBDoesNotFindServiceFromTenantA() {
    UUID serviceId = UUID.randomUUID();
    TenantContext tenants = mock(TenantContext.class);
    OfferedServiceRepository repository = mock(OfferedServiceRepository.class);
    when(tenants.getRequiredTenantId()).thenReturn(202L);
    when(repository.findByIdAndTenantId(serviceId, 202L)).thenReturn(Optional.empty());
    OfferedServiceUseCase useCase =
        new OfferedServiceUseCase(
            tenants, repository, mock(OutboxWriter.class), mock(CatalogCacheInvalidator.class));

    assertThatThrownBy(() -> useCase.get(serviceId)).isInstanceOf(ResourceNotFoundException.class);
    verify(repository).findByIdAndTenantId(serviceId, 202L);
  }
}
