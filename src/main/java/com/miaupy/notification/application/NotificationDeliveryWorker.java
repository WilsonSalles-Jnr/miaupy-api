package com.miaupy.notification.application;

import com.miaupy.notification.domain.Notification;
import com.miaupy.notification.domain.NotificationChannel;
import com.miaupy.notification.domain.NotificationRepository;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationDeliveryWorker {
  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationDeliveryWorker.class);
  private final NotificationRepository repository;
  private final Map<NotificationChannel, NotificationDelivery> deliveries;
  private final int maxAttempts;

  public NotificationDeliveryWorker(
      NotificationRepository repository,
      List<NotificationDelivery> deliveries,
      @Value("${miaupy.notification.max-attempts:5}") int maxAttempts) {
    this.repository = repository;
    this.deliveries = new EnumMap<>(NotificationChannel.class);
    deliveries.forEach(delivery -> this.deliveries.put(delivery.channel(), delivery));
    this.maxAttempts = maxAttempts;
  }

  @Scheduled(fixedDelayString = "${miaupy.notification.delivery-delay:PT5S}")
  @Transactional
  public void deliverPending() {
    for (Notification notification : repository.lockPendingBatch(Instant.now())) {
      NotificationDelivery delivery = deliveries.get(notification.channel());
      if (delivery == null) {
        repository.save(notification.skipped("CHANNEL_NOT_CONFIGURED"));
        continue;
      }
      try {
        delivery.send(notification);
        repository.save(notification.sent());
      } catch (RuntimeException exception) {
        repository.save(notification.failed(maxAttempts, exception.getClass().getSimpleName()));
        LOGGER.warn(
            "Notification delivery failed notificationId={} channel={} attempt={}",
            notification.id(),
            notification.channel(),
            notification.attempts() + 1);
      }
    }
  }
}
