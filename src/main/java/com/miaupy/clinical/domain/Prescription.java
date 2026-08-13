package com.miaupy.clinical.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Prescription(
    UUID id,
    Long tenantId,
    UUID tenantPetId,
    UUID consultationId,
    String medication,
    String dosage,
    String frequency,
    String duration,
    String instructions,
    Instant issuedAt,
    LocalDate validUntil,
    String veterinarianSubject,
    Instant createdAt,
    Long version) {}
