package com.miaupy.customer.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.miaupy.customer.domain.*;
import com.miaupy.shared.exception.ResourceNotFoundException;
import com.miaupy.shared.tenant.TenantContext;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantCustomerIsolationTest {
  @Test
  void tenantBDoesNotFindCustomerFromTenantA() {
    UUID customerId = UUID.randomUUID();
    TenantContext context = mock(TenantContext.class);
    TenantCustomerRepository repository = mock(TenantCustomerRepository.class);
    when(context.getRequiredTenantId()).thenReturn(202L);
    when(repository.findByIdAndTenantId(customerId, 202L)).thenReturn(Optional.empty());
    TenantCustomerUseCase useCase = new TenantCustomerUseCase(context, repository);
    assertThatThrownBy(() -> useCase.get(customerId)).isInstanceOf(ResourceNotFoundException.class);
    verify(repository).findByIdAndTenantId(customerId, 202L);
    verify(repository, never()).findByIdAndTenantId(customerId, 101L);
  }
}
