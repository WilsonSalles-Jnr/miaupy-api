package com.miaupy.clinical.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ClinicalHistoryEvent(
    UUID id,
    Long tenantId,
    UUID tenantPetId,
    String eventType,
    UUID resourceId,
    String summary,
    Instant occurredAt,
    String recordedBy,
    String recordedByName,
    Map<String, Object> details,
    Instant createdAt) {}
