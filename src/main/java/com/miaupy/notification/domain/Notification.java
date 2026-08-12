package com.miaupy.notification.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record Notification(
    UUID id,
    Long tenantId,
    UUID consumerProfileId,
    UUID sourceEventId,
    String deduplicationKey,
    String type,
    NotificationChannel channel,
    String recipient,
    String subject,
    String body,
    NotificationStatus status,
    int attempts,
    Instant nextAttemptAt,
    Instant sentAt,
    String lastError,
    Instant createdAt,
    Instant updatedAt,
    Long version) {

  public static Notification email(
      Long tenantId,
      UUID consumerProfileId,
      UUID sourceEventId,
      String deduplicationKey,
      String type,
      String recipient,
      String subject,
      String body) {
    if (recipient == null || recipient.isBlank()) {
      throw new IllegalArgumentException("Notification recipient is required");
    }
    Instant now = Instant.now();
    return new Notification(
        UUID.randomUUID(),
        tenantId,
        consumerProfileId,
        sourceEventId,
        deduplicationKey,
        type,
        NotificationChannel.EMAIL,
        recipient.strip().toLowerCase(),
        subject,
        body,
        NotificationStatus.PENDING,
        0,
        now,
        null,
        null,
        now,
        now,
        null);
  }

  public Notification sent() {
    Instant now = Instant.now();
    return copy(NotificationStatus.SENT, attempts + 1, now, null, now);
  }

  public Notification failed(int maxAttempts, String safeError) {
    int newAttempts = attempts + 1;
    boolean exhausted = newAttempts >= maxAttempts;
    Instant retryAt =
        exhausted
            ? nextAttemptAt
            : Instant.now().plus(Duration.ofMinutes(1L << Math.min(newAttempts, 5)));
    return copy(
        exhausted ? NotificationStatus.FAILED : NotificationStatus.PENDING,
        newAttempts,
        null,
        safeError,
        retryAt);
  }

  public Notification skipped(String reason) {
    return copy(NotificationStatus.SKIPPED, attempts, null, reason, nextAttemptAt);
  }

  private Notification copy(
      NotificationStatus newStatus,
      int newAttempts,
      Instant newSentAt,
      String newLastError,
      Instant retryAt) {
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
        newStatus,
        newAttempts,
        retryAt,
        newSentAt,
        newLastError,
        createdAt,
        Instant.now(),
        version);
  }
}
