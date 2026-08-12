package com.miaupy.notification.infrastructure.persistence;

import com.miaupy.notification.domain.Notification;
import com.miaupy.notification.domain.NotificationChannel;
import com.miaupy.notification.domain.NotificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification", schema = "integration")
class NotificationJpaEntity {
  @Id UUID id;

  @Column(name = "tenant_id")
  Long tenantId;

  @Column(name = "consumer_profile_id")
  UUID consumerProfileId;

  @Column(name = "source_event_id", nullable = false)
  UUID sourceEventId;

  @Column(name = "deduplication_key", nullable = false, length = 255)
  String deduplicationKey;

  @Column(name = "notification_type", nullable = false, length = 80)
  String type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  NotificationChannel channel;

  @Column(nullable = false, length = 320)
  String recipient;

  @Column(nullable = false, length = 200)
  String subject;

  @Column(nullable = false, length = 5000)
  String body;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  NotificationStatus status;

  @Column(nullable = false)
  int attempts;

  @Column(name = "next_attempt_at", nullable = false)
  Instant nextAttemptAt;

  @Column(name = "sent_at")
  Instant sentAt;

  @Column(name = "last_error", length = 1000)
  String lastError;

  @Column(name = "created_at", nullable = false, updatable = false)
  Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  Instant updatedAt;

  @Version Long version;

  protected NotificationJpaEntity() {}

  NotificationJpaEntity(Notification notification) {
    id = notification.id();
    tenantId = notification.tenantId();
    consumerProfileId = notification.consumerProfileId();
    sourceEventId = notification.sourceEventId();
    deduplicationKey = notification.deduplicationKey();
    type = notification.type();
    channel = notification.channel();
    recipient = notification.recipient();
    subject = notification.subject();
    body = notification.body();
    status = notification.status();
    attempts = notification.attempts();
    nextAttemptAt = notification.nextAttemptAt();
    sentAt = notification.sentAt();
    lastError = notification.lastError();
    createdAt = notification.createdAt();
    updatedAt = notification.updatedAt();
    version = notification.version();
  }

  Notification toDomain() {
    return new Notification(
        id,
        tenantId,
        consumerProfileId,
        sourceEventId,
        deduplicationKey,
        type,
        channel,
        recipient,
        subject,
        body,
        status,
        attempts,
        nextAttemptAt,
        sentAt,
        lastError,
        createdAt,
        updatedAt,
        version);
  }
}
