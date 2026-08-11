package com.miaupy.consumer.domain;

import com.miaupy.pet.domain.PetSex;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ConsumerPet(UUID id, UUID consumerProfileId, String name, String species, String breed,
                          LocalDate birthDate, PetSex sex, BigDecimal weight, String color, String microchip,
                          boolean neutered, boolean active, Instant deletedAt, Instant createdAt, Instant updatedAt,
                          Long version) {
    public static ConsumerPet create(UUID ownerId, String name, String species, String breed, LocalDate birthDate,
                                     PetSex sex, BigDecimal weight, String color, String microchip, boolean neutered) {
        Instant now = Instant.now();
        return new ConsumerPet(UUID.randomUUID(), ownerId, name, species, breed, birthDate, sex, weight, color,
                microchip, neutered, true, null, now, now, null);
    }
    public ConsumerPet update(String name, String species, String breed, LocalDate birthDate, PetSex sex,
                              BigDecimal weight, String color, String microchip, boolean neutered) {
        return new ConsumerPet(id, consumerProfileId, name, species, breed, birthDate, sex, weight, color, microchip,
                neutered, active, deletedAt, createdAt, Instant.now(), version);
    }
    public ConsumerPet deactivate() {
        Instant now = Instant.now();
        return new ConsumerPet(id, consumerProfileId, name, species, breed, birthDate, sex, weight, color, microchip,
                neutered, false, now, createdAt, now, version);
    }
}
