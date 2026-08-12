package com.miaupy.notification.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miaupy.consumer.application.ConsumerProfileUseCase;
import com.miaupy.notification.domain.NotificationRepository;
import com.miaupy.shared.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class NotificationTenantIsolationTest {
  @Test
  void businessNotificationsAlwaysUseTenantFromJwt() {
    NotificationRepository repository = mock(NotificationRepository.class);
    TenantContext tenants = mock(TenantContext.class);
    when(tenants.getRequiredTenantId()).thenReturn(202L);
    when(repository.findAllByTenantId(
            org.mockito.ArgumentMatchers.eq(202L), org.mockito.ArgumentMatchers.any()))
        .thenReturn(Page.empty());
    NotificationQueryUseCase useCase =
        new NotificationQueryUseCase(repository, mock(ConsumerProfileUseCase.class), tenants);

    useCase.listBusiness(0, 20);

    verify(repository)
        .findAllByTenantId(
            org.mockito.ArgumentMatchers.eq(202L),
            org.mockito.ArgumentMatchers.any(Pageable.class));
  }
}
