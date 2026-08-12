package com.miaupy.notification.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationRepository {
  Notification save(Notification notification);

  boolean existsByDeduplicationKey(String key);

  List<Notification> lockPendingBatch(Instant dueAt);

  Page<Notification> findAllByTenantId(Long tenantId, Pageable pageable);

  Page<Notification> findAllByConsumerProfileId(UUID consumerProfileId, Pageable pageable);
}
