package com.miaupy.pet.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TenantPet(
    UUID id,
    Long tenantId,
    UUID tenantCustomerId,
    UUID consumerPetId,
    String name,
    String species,
    String breed,
    LocalDate birthDate,
    PetSex sex,
    BigDecimal weight,
    String notes,
    boolean active,
    Instant deletedAt,
    Instant createdAt,
    Instant updatedAt,
    Long version) {
  public static TenantPet create(
      Long tenant,
      UUID customer,
      String name,
      String species,
      String breed,
      LocalDate birthDate,
      PetSex sex,
      BigDecimal weight,
      String notes) {
    Instant now = Instant.now();
    return new TenantPet(
        UUID.randomUUID(),
        tenant,
        customer,
        null,
        name,
        species,
        breed,
        birthDate,
        sex,
        weight,
        notes,
        true,
        null,
        now,
        now,
        null);
  }

  public TenantPet update(
      String name,
      String species,
      String breed,
      LocalDate birthDate,
      PetSex sex,
      BigDecimal weight,
      String notes) {
    return new TenantPet(
        id,
        tenantId,
        tenantCustomerId,
        consumerPetId,
        name,
        species,
        breed,
        birthDate,
        sex,
        weight,
        notes,
        active,
        deletedAt,
        createdAt,
        Instant.now(),
        version);
  }

  public TenantPet deactivate() {
    Instant now = Instant.now();
    return new TenantPet(
        id,
        tenantId,
        tenantCustomerId,
        consumerPetId,
        name,
        species,
        breed,
        birthDate,
        sex,
        weight,
        notes,
        false,
        now,
        createdAt,
        now,
        version);
  }
}
