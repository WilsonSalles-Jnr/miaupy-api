package com.miaupy.clinical.domain;

import java.time.Instant;
import java.util.UUID;

public record MedicalRecord(
    UUID id,
    Long tenantId,
    UUID tenantPetId,
    String allergies,
    String chronicConditions,
    String currentMedications,
    String notes,
    String createdBy,
    String updatedBy,
    Instant createdAt,
    Instant updatedAt,
    Long version) {}
