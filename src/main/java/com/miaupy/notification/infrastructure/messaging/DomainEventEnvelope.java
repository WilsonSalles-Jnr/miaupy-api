package com.miaupy.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

record DomainEventEnvelope(
    UUID eventId,
    String eventType,
    int eventVersion,
    Instant occurredAt,
    Long tenantId,
    JsonNode actor,
    JsonNode payload) {}
