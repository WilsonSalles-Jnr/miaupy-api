package com.miaupy.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationTest {
  @Test
  void normalizesEmailAndMarksSuccessfulDelivery() {
    Notification notification = notification();

    Notification sent = notification.sent();

    assertThat(notification.recipient()).isEqualTo("consumer@example.com");
    assertThat(sent.status()).isEqualTo(NotificationStatus.SENT);
    assertThat(sent.attempts()).isEqualTo(1);
    assertThat(sent.sentAt()).isNotNull();
  }

  @Test
  void retriesWithBackoffAndStopsAtMaximumAttempts() {
    Notification firstFailure = notification().failed(2, "MailException");
    Notification exhausted = firstFailure.failed(2, "MailException");

    assertThat(firstFailure.status()).isEqualTo(NotificationStatus.PENDING);
    assertThat(firstFailure.nextAttemptAt()).isAfter(firstFailure.updatedAt());
    assertThat(exhausted.status()).isEqualTo(NotificationStatus.FAILED);
    assertThat(exhausted.attempts()).isEqualTo(2);
  }

  private Notification notification() {
    return Notification.email(
        101L,
        UUID.randomUUID(),
        UUID.randomUUID(),
        "dedup-key",
        "order.created",
        " Consumer@Example.com ",
        "Order created",
        "Your order was created");
  }
}
