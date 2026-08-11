package com.miaupy.consumer.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ConsumerProfile(
        UUID id,
        String authSubject,
        String name,
        String email,
        String phone,
        String document,
        LocalDate birthDate,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
    public static ConsumerProfile create(String subject, String name, String email, String phone, String document,
                                         LocalDate birthDate) {
        Instant now = Instant.now();
        return new ConsumerProfile(UUID.randomUUID(), subject, name, email, phone, document, birthDate, true,
                now, now, null);
    }

    public ConsumerProfile update(String name, String email, String phone, String document, LocalDate birthDate) {
        return new ConsumerProfile(id, authSubject, name, email, phone, document, birthDate, active, createdAt,
                Instant.now(), version);
    }
}
