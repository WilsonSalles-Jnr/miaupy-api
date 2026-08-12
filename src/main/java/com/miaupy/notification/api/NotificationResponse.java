package com.miaupy.notification.api;

import com.miaupy.notification.domain.Notification;
import com.miaupy.notification.domain.NotificationChannel;
import com.miaupy.notification.domain.NotificationStatus;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    String type,
    NotificationChannel channel,
    String subject,
    String body,
    NotificationStatus status,
    Instant sentAt,
    Instant createdAt) {
  static NotificationResponse from(Notification notification) {
    return new NotificationResponse(
        notification.id(),
        notification.type(),
        notification.channel(),
        notification.subject(),
        notification.body(),
        notification.status(),
        notification.sentAt(),
        notification.createdAt());
  }
}
