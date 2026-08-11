package com.miaupy.integration.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miaupy.shared.security.ActorContext;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OutboxWriter {

  private final ActorContext actorContext;
  private final ObjectMapper objectMapper;
  private final OutboxEventRepository repository;

  public OutboxWriter(
      ActorContext actorContext, ObjectMapper objectMapper, OutboxEventRepository repository) {
    this.actorContext = actorContext;
    this.objectMapper = objectMapper;
    this.repository = repository;
  }

  public void append(
      String aggregateType,
      UUID aggregateId,
      String eventType,
      Long tenantId,
      Map<String, ?> payload) {
    var actor = actorContext.getRequiredActor();
    try {
      repository.save(
          new OutboxEvent(
              UUID.randomUUID(),
              aggregateType,
              aggregateId,
              eventType,
              1,
              Instant.now(),
              tenantId,
              actor.type().name(),
              actor.subject(),
              objectMapper.writeValueAsString(payload)));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize domain event payload", exception);
    }
  }
}
