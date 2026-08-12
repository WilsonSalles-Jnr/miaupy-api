package com.miaupy.notification.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miaupy.notification.application.NotificationComposer;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class NotificationDomainEventHandler {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(NotificationDomainEventHandler.class);
  private final ObjectMapper objectMapper;
  private final ProcessedEventGuard processedEvents;
  private final NotificationComposer composer;

  NotificationDomainEventHandler(
      ObjectMapper objectMapper,
      ProcessedEventGuard processedEvents,
      NotificationComposer composer) {
    this.objectMapper = objectMapper;
    this.processedEvents = processedEvents;
    this.composer = composer;
  }

  @KafkaListener(topics = "${miaupy.kafka.domain-events-topic:miaupy.domain-events}")
  @Transactional
  public void listen(String value) {
    DomainEventEnvelope event;
    try {
      event = objectMapper.readValue(value, DomainEventEnvelope.class);
    } catch (JsonProcessingException exception) {
      LOGGER.warn("Discarding malformed domain event from notification topic");
      return;
    }
    handle(event);
  }

  void handle(DomainEventEnvelope event) {
    if (event.eventId() == null || event.eventType() == null) return;
    if (!processedEvents.acquire(event.eventId())) return;
    UUID aggregateId = aggregateId(event);
    if (aggregateId != null) {
      composer.compose(event.eventId(), event.eventType(), event.tenantId(), aggregateId);
    }
  }

  private UUID aggregateId(DomainEventEnvelope event) {
    String field =
        event.eventType().startsWith("appointment.")
            ? "appointmentId"
            : event.eventType().startsWith("order.") ? "orderId" : null;
    if (field == null || event.payload() == null || !event.payload().hasNonNull(field)) return null;
    try {
      return UUID.fromString(event.payload().get(field).asText());
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }
}
