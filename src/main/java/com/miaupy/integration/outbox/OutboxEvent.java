package com.miaupy.integration.outbox;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(
    UUID eventId,
    String aggregateType,
    UUID aggregateId,
    String eventType,
    int eventVersion,
    Instant occurredAt,
    Long tenantId,
    String actorType,
    String actorId,
    String payload) {}
