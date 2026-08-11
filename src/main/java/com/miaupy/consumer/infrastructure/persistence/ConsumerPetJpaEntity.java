package com.miaupy.consumer.infrastructure.persistence;

import com.miaupy.consumer.domain.ConsumerPet;
import com.miaupy.pet.domain.PetSex;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "consumer_pet", schema = "consumer")
class ConsumerPetJpaEntity {
  @Id UUID id;

  @Column(name = "consumer_profile_id", nullable = false)
  UUID consumerProfileId;

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

  @Column(length = 80)
  String color;

  @Column(length = 80)
  String microchip;

  @Column(nullable = false)
  boolean neutered;

  @Column(nullable = false)
  boolean active;

  @Column(name = "deleted_at")
  Instant deletedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  Instant updatedAt;

  @Version Long version;

  protected ConsumerPetJpaEntity() {}

  ConsumerPetJpaEntity(ConsumerPet p) {
    id = p.id();
    consumerProfileId = p.consumerProfileId();
    name = p.name();
    species = p.species();
    breed = p.breed();
    birthDate = p.birthDate();
    sex = p.sex();
    weight = p.weight();
    color = p.color();
    microchip = p.microchip();
    neutered = p.neutered();
    active = p.active();
    deletedAt = p.deletedAt();
    createdAt = p.createdAt();
    updatedAt = p.updatedAt();
    version = p.version();
  }
}
