package com.miaupy.notification.infrastructure.persistence;

import com.miaupy.notification.domain.Notification;
import com.miaupy.notification.domain.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

interface SpringDataNotificationRepository extends JpaRepository<NotificationJpaEntity, UUID> {
  boolean existsByDeduplicationKey(String key);

  @Query(
      value =
          "SELECT * FROM integration.notification WHERE status = 'PENDING' "
              + "AND next_attempt_at <= :dueAt ORDER BY next_attempt_at LIMIT 50 FOR UPDATE SKIP LOCKED",
      nativeQuery = true)
  List<NotificationJpaEntity> lockPendingBatch(@Param("dueAt") Instant dueAt);

  Page<NotificationJpaEntity> findAllByTenantId(Long tenantId, Pageable pageable);

  Page<NotificationJpaEntity> findAllByConsumerProfileId(UUID consumerProfileId, Pageable pageable);
}

@Repository
class NotificationRepositoryAdapter implements NotificationRepository {
  private final SpringDataNotificationRepository repository;

  NotificationRepositoryAdapter(SpringDataNotificationRepository repository) {
    this.repository = repository;
  }

  public Notification save(Notification notification) {
    return repository.save(new NotificationJpaEntity(notification)).toDomain();
  }

  public boolean existsByDeduplicationKey(String key) {
    return repository.existsByDeduplicationKey(key);
  }

  public List<Notification> lockPendingBatch(Instant dueAt) {
    return repository.lockPendingBatch(dueAt).stream()
        .map(NotificationJpaEntity::toDomain)
        .toList();
  }

  public Page<Notification> findAllByTenantId(Long tenantId, Pageable pageable) {
    return repository.findAllByTenantId(tenantId, pageable).map(NotificationJpaEntity::toDomain);
  }

  public Page<Notification> findAllByConsumerProfileId(UUID consumerProfileId, Pageable pageable) {
    return repository
        .findAllByConsumerProfileId(consumerProfileId, pageable)
        .map(NotificationJpaEntity::toDomain);
  }
}
