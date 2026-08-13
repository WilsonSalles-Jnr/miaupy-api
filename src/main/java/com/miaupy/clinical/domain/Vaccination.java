package com.miaupy.clinical.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Vaccination(
    UUID id,
    Long tenantId,
    UUID tenantPetId,
    String vaccineName,
    String manufacturer,
    String batchNumber,
    LocalDate administeredOn,
    LocalDate nextDueOn,
    String veterinarianSubject,
    String notes,
    Instant createdAt,
    Long version) {}
