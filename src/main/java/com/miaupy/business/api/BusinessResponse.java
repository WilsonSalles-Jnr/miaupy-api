package com.miaupy.business.api;

import com.miaupy.business.domain.Business;
import java.time.Instant;
import java.util.UUID;

public record BusinessResponse(
    UUID id,
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
    Instant updatedAt) {
  static BusinessResponse from(Business business) {
    return new BusinessResponse(
        business.id(),
        business.slug(),
        business.name(),
        business.tradeName(),
        business.document(),
        business.description(),
        business.phone(),
        business.email(),
        business.website(),
        business.active(),
        business.publicVisible(),
        business.createdAt(),
        business.updatedAt());
  }
}
