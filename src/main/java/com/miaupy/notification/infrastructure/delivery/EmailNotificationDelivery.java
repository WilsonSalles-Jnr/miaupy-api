package com.miaupy.notification.infrastructure.delivery;

import com.miaupy.notification.application.NotificationDelivery;
import com.miaupy.notification.domain.Notification;
import com.miaupy.notification.domain.NotificationChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
class EmailNotificationDelivery implements NotificationDelivery {
  private final JavaMailSender mailSender;
  private final String from;

  EmailNotificationDelivery(
      JavaMailSender mailSender,
      @Value("${miaupy.notification.from:no-reply@miaupy.local}") String from) {
    this.mailSender = mailSender;
    this.from = from;
  }

  public NotificationChannel channel() {
    return NotificationChannel.EMAIL;
  }

  public void send(Notification notification) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(notification.recipient());
    message.setSubject(notification.subject());
    message.setText(notification.body());
    mailSender.send(message);
  }
}
