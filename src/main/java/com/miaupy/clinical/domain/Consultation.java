package com.miaupy.clinical.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Consultation(
    UUID id,
    Long tenantId,
    UUID tenantPetId,
    UUID appointmentId,
    Instant occurredAt,
    String reason,
    String anamnesis,
    String clinicalFindings,
    String diagnosis,
    String treatmentPlan,
    BigDecimal weight,
    BigDecimal temperature,
    String veterinarianSubject,
    Instant createdAt,
    Long version) {}
