package com.miaupy.notification.application;

import com.miaupy.consumer.application.ConsumerProfileUseCase;
import com.miaupy.notification.domain.Notification;
import com.miaupy.notification.domain.NotificationRepository;
import com.miaupy.shared.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationQueryUseCase {
  private final NotificationRepository notifications;
  private final ConsumerProfileUseCase profiles;
  private final TenantContext tenants;

  public NotificationQueryUseCase(
      NotificationRepository notifications,
      ConsumerProfileUseCase profiles,
      TenantContext tenants) {
    this.notifications = notifications;
    this.profiles = profiles;
    this.tenants = tenants;
  }

  @Transactional
  public Page<Notification> listConsumer(int page, int size) {
    return notifications.findAllByConsumerProfileId(profiles.getMe().id(), page(page, size));
  }

  @Transactional(readOnly = true)
  public Page<Notification> listBusiness(int page, int size) {
    return notifications.findAllByTenantId(tenants.getRequiredTenantId(), page(page, size));
  }

  private PageRequest page(int page, int size) {
    return PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
  }
}
