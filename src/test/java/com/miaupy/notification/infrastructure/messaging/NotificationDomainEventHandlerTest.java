package com.miaupy.notification.infrastructure.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.miaupy.notification.application.NotificationComposer;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationDomainEventHandlerTest {
  @Test
  void duplicateKafkaEventIsIgnored() {
    ProcessedEventGuard guard = mock(ProcessedEventGuard.class);
    NotificationComposer composer = mock(NotificationComposer.class);
    UUID eventId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    when(guard.acquire(eventId)).thenReturn(false);
    NotificationDomainEventHandler handler =
        new NotificationDomainEventHandler(new ObjectMapper(), guard, composer);
    DomainEventEnvelope event =
        new DomainEventEnvelope(
            eventId,
            "order.created",
            1,
            Instant.now(),
            101L,
            JsonNodeFactory.instance.objectNode(),
            JsonNodeFactory.instance.objectNode().put("orderId", orderId.toString()));

    handler.handle(event);

    verify(composer, never()).compose(eventId, "order.created", 101L, orderId);
  }

  @Test
  void firstKafkaDeliveryCreatesNotificationFromAllowlistedEvent() {
    ProcessedEventGuard guard = mock(ProcessedEventGuard.class);
    NotificationComposer composer = mock(NotificationComposer.class);
    UUID eventId = UUID.randomUUID();
    UUID appointmentId = UUID.randomUUID();
    when(guard.acquire(eventId)).thenReturn(true);
    NotificationDomainEventHandler handler =
        new NotificationDomainEventHandler(new ObjectMapper(), guard, composer);
    DomainEventEnvelope event =
        new DomainEventEnvelope(
            eventId,
            "appointment.confirmed",
            1,
            Instant.now(),
            101L,
            JsonNodeFactory.instance.objectNode(),
            JsonNodeFactory.instance.objectNode().put("appointmentId", appointmentId.toString()));

    handler.handle(event);

    verify(composer).compose(eventId, "appointment.confirmed", 101L, appointmentId);
  }
}
