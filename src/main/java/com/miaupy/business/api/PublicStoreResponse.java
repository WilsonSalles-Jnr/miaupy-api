package com.miaupy.business.api;

import com.miaupy.business.domain.Business;
import java.util.UUID;

public record PublicStoreResponse(
    UUID id,
    String slug,
    String name,
    String tradeName,
    String description,
    String phone,
    String email,
    String website) {
  static PublicStoreResponse from(Business business) {
    return new PublicStoreResponse(
        business.id(),
        business.slug(),
        business.name(),
        business.tradeName(),
        business.description(),
        business.phone(),
        business.email(),
        business.website());
  }
}
