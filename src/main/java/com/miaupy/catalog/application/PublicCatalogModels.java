package com.miaupy.catalog.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class PublicCatalogModels {
  private PublicCatalogModels() {}

  public record PublicProduct(
      UUID id,
      String sku,
      String name,
      String description,
      BigDecimal price,
      BigDecimal promotionalPrice,
      int stockQuantity) {}

  public record PublicService(
      UUID id,
      String name,
      String description,
      int durationMinutes,
      BigDecimal price,
      boolean requiresApproval) {}

  public record ProductPage(
      List<PublicProduct> content, int page, int size, long totalElements, int totalPages) {}

  public record ServicePage(
      List<PublicService> content, int page, int size, long totalElements, int totalPages) {}
}
