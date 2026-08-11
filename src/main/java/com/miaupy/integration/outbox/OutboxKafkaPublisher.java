package com.miaupy.integration.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class OutboxKafkaPublisher {
  private static final Logger LOGGER = LoggerFactory.getLogger(OutboxKafkaPublisher.class);
  private final SpringDataOutboxEventRepository repository;
  private final KafkaTemplate<String, String> kafka;
  private final ObjectMapper objectMapper;
  private final String topic;

  OutboxKafkaPublisher(
      SpringDataOutboxEventRepository repository,
      KafkaTemplate<String, String> kafka,
      ObjectMapper objectMapper,
      @Value("${miaupy.kafka.domain-events-topic:miaupy.domain-events}") String topic) {
    this.repository = repository;
    this.kafka = kafka;
    this.objectMapper = objectMapper;
    this.topic = topic;
  }

  @Scheduled(fixedDelayString = "${miaupy.outbox.publish-delay:PT2S}")
  @Transactional
  public void publishPending() {
    for (OutboxEventJpaEntity event : repository.lockPendingBatch()) {
      try {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("eventId", event.id);
        value.put("eventType", event.eventType);
        value.put("eventVersion", event.eventVersion);
        value.put("occurredAt", event.occurredAt);
        value.put("tenantId", event.tenantId);
        value.put("actor", Map.of("type", event.actorType, "id", event.actorId));
        value.put("payload", objectMapper.readTree(event.payload));
        String envelope = objectMapper.writeValueAsString(value);
        kafka.send(topic, event.aggregateId.toString(), envelope).get(10, TimeUnit.SECONDS);
        event.published();
      } catch (Exception exception) {
        event.failed(exception);
        LOGGER.warn("Failed to publish outbox event eventId={}", event.id);
      }
    }
  }
}
