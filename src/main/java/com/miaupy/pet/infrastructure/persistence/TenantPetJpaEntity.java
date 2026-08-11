package com.miaupy.pet.infrastructure.persistence;

import com.miaupy.pet.domain.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tenant_pet", schema = "pet")
class TenantPetJpaEntity {
  @Id UUID id;

  @Column(name = "tenant_id", nullable = false)
  Long tenantId;

  @Column(name = "tenant_customer_id", nullable = false)
  UUID tenantCustomerId;

  @Column(name = "consumer_pet_id")
  UUID consumerPetId;

  @Column(nullable = false, length = 120)
  String name;

  @Column(nullable = false, length = 60)
  String species;

  @Column(length = 120)
  String breed;

  @Column(name = "birth_date")
  LocalDate birthDate;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  PetSex sex;

  @Column(precision = 8, scale = 2)
  BigDecimal weight;

  @Column(length = 2000)
  String notes;

  @Column(nullable = false)
  boolean active;

  @Column(name = "deleted_at")
  Instant deletedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  Instant updatedAt;

  @Version Long version;

  protected TenantPetJpaEntity() {}

  TenantPetJpaEntity(TenantPet p) {
    id = p.id();
    tenantId = p.tenantId();
    tenantCustomerId = p.tenantCustomerId();
    consumerPetId = p.consumerPetId();
    name = p.name();
    species = p.species();
    breed = p.breed();
    birthDate = p.birthDate();
    sex = p.sex();
    weight = p.weight();
    notes = p.notes();
    active = p.active();
    deletedAt = p.deletedAt();
    createdAt = p.createdAt();
    updatedAt = p.updatedAt();
    version = p.version();
  }
}
