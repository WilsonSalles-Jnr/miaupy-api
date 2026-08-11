package com.miaupy.business.domain;

import java.time.Instant;
import java.util.UUID;

public final class Business {

  private final UUID id;
  private final Long tenantId;
  private String slug;
  private String name;
  private String tradeName;
  private String document;
  private String description;
  private String phone;
  private String email;
  private String website;
  private boolean active;
  private boolean publicVisible;
  private final Instant createdAt;
  private Instant updatedAt;
  private final Long version;

  private Business(
      UUID id,
      Long tenantId,
      String slug,
      String name,
      String tradeName,
      String document,
      String description,
      String phone,
      String email,
      String website,
      boolean active,
      boolean publicVisible,
      Instant createdAt,
      Instant updatedAt,
      Long version) {
    this.id = id;
    this.tenantId = tenantId;
    this.slug = slug;
    this.name = name;
    this.tradeName = tradeName;
    this.document = document;
    this.description = description;
    this.phone = phone;
    this.email = email;
    this.website = website;
    this.active = active;
    this.publicVisible = publicVisible;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.version = version;
  }

  public static Business create(
      Long tenantId,
      String slug,
      String name,
      String tradeName,
      String document,
      String description,
      String phone,
      String email,
      String website) {
    Instant now = Instant.now();
    return new Business(
        UUID.randomUUID(),
        tenantId,
        slug,
        name,
        tradeName,
        document,
        description,
        phone,
        email,
        website,
        true,
        false,
        now,
        now,
        null);
  }

  public static Business restore(
      UUID id,
      Long tenantId,
      String slug,
      String name,
      String tradeName,
      String document,
      String description,
      String phone,
      String email,
      String website,
      boolean active,
      boolean publicVisible,
      Instant createdAt,
      Instant updatedAt,
      Long version) {
    return new Business(
        id,
        tenantId,
        slug,
        name,
        tradeName,
        document,
        description,
        phone,
        email,
        website,
        active,
        publicVisible,
        createdAt,
        updatedAt,
        version);
  }

  public void update(
      String slug,
      String name,
      String tradeName,
      String document,
      String description,
      String phone,
      String email,
      String website,
      boolean publicVisible) {
    this.slug = slug;
    this.name = name;
    this.tradeName = tradeName;
    this.document = document;
    this.description = description;
    this.phone = phone;
    this.email = email;
    this.website = website;
    this.publicVisible = publicVisible;
    this.updatedAt = Instant.now();
  }

  public UUID id() {
    return id;
  }

  public Long tenantId() {
    return tenantId;
  }

  public String slug() {
    return slug;
  }

  public String name() {
    return name;
  }

  public String tradeName() {
    return tradeName;
  }

  public String document() {
    return document;
  }

  public String description() {
    return description;
  }

  public String phone() {
    return phone;
  }

  public String email() {
    return email;
  }

  public String website() {
    return website;
  }

  public boolean active() {
    return active;
  }

  public boolean publicVisible() {
    return publicVisible;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public Long version() {
    return version;
  }
}
