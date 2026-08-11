package com.miaupy.consumer.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "consumer_profile", schema = "consumer")
class ConsumerProfileJpaEntity {
  @Id UUID id;

  @Column(name = "auth_subject", nullable = false, unique = true, length = 160)
  String authSubject;

  @Column(nullable = false, length = 160)
  String name;

  @Column(nullable = false, length = 254)
  String email;

  @Column(length = 32)
  String phone;

  @Column(length = 32)
  String document;

  @Column(name = "birth_date")
  LocalDate birthDate;

  @Column(nullable = false)
  boolean active;

  @Column(name = "created_at", nullable = false, updatable = false)
  Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  Instant updatedAt;

  @Version Long version;

  protected ConsumerProfileJpaEntity() {}

  ConsumerProfileJpaEntity(
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
      Long version) {
    this.id = id;
    this.authSubject = authSubject;
    this.name = name;
    this.email = email;
    this.phone = phone;
    this.document = document;
    this.birthDate = birthDate;
    this.active = active;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.version = version;
  }
}
