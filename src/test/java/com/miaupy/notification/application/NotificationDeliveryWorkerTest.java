package com.miaupy.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miaupy.notification.domain.Notification;
import com.miaupy.notification.domain.NotificationChannel;
import com.miaupy.notification.domain.NotificationRepository;
import com.miaupy.notification.domain.NotificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class NotificationDeliveryWorkerTest {
  @Test
  void deliversPendingNotificationAndPersistsSentStatus() {
    NotificationRepository repository = mock(NotificationRepository.class);
    NotificationDelivery delivery = mock(NotificationDelivery.class);
    Notification notification = notification();
    when(delivery.channel()).thenReturn(NotificationChannel.EMAIL);
    when(repository.lockPendingBatch(any(Instant.class))).thenReturn(List.of(notification));
    AtomicReference<Notification> saved = new AtomicReference<>();
    when(repository.save(any()))
        .thenAnswer(invocation -> saved.getAndSet(invocation.getArgument(0)));
    NotificationDeliveryWorker worker =
        new NotificationDeliveryWorker(repository, List.of(delivery), 3);

    worker.deliverPending();

    verify(delivery).send(notification);
    assertThat(saved.get().status()).isEqualTo(NotificationStatus.SENT);
  }

  private Notification notification() {
    return Notification.email(
        101L,
        UUID.randomUUID(),
        UUID.randomUUID(),
        "key",
        "order.created",
        "consumer@example.com",
        "Subject",
        "Body");
  }
}
