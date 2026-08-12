package com.miaupy.notification.application;

import com.miaupy.notification.domain.Notification;
import com.miaupy.notification.domain.NotificationChannel;

public interface NotificationDelivery {
  NotificationChannel channel();

  void send(Notification notification);
}
