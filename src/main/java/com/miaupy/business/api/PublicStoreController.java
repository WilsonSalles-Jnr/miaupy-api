package com.miaupy.business.api;

import com.miaupy.business.application.GetPublicBusinessUseCase;
import com.miaupy.catalog.application.PublicCatalogModels.ProductPage;
import com.miaupy.catalog.application.PublicCatalogModels.PublicProduct;
import com.miaupy.catalog.application.PublicCatalogModels.ServicePage;
import com.miaupy.catalog.application.PublicCatalogUseCase;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/stores")
public class PublicStoreController {

  private final GetPublicBusinessUseCase getPublicBusiness;
  private final PublicCatalogUseCase publicCatalog;

  public PublicStoreController(
      GetPublicBusinessUseCase getPublicBusiness, PublicCatalogUseCase publicCatalog) {
    this.getPublicBusiness = getPublicBusiness;
    this.publicCatalog = publicCatalog;
  }

  @GetMapping("/{slug}")
  public PublicStoreResponse getBySlug(@PathVariable String slug) {
    return PublicStoreResponse.from(getPublicBusiness.execute(slug));
  }

  @GetMapping("/{slug}/products")
  public ProductPage products(
      @PathVariable String slug,
      @RequestParam(defaultValue = "0") @PositiveOrZero int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return publicCatalog.products(slug, page, size);
  }

  @GetMapping("/{slug}/products/{productId}")
  public PublicProduct product(@PathVariable String slug, @PathVariable UUID productId) {
    return publicCatalog.product(slug, productId);
  }

  @GetMapping("/{slug}/services")
  public ServicePage services(
      @PathVariable String slug,
      @RequestParam(defaultValue = "0") @PositiveOrZero int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return publicCatalog.services(slug, page, size);
  }
}
