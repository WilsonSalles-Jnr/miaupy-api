package com.miaupy.catalog.application;

import com.miaupy.business.domain.Business;
import com.miaupy.business.domain.BusinessRepository;
import com.miaupy.catalog.application.PublicCatalogModels.ProductPage;
import com.miaupy.catalog.application.PublicCatalogModels.PublicProduct;
import com.miaupy.catalog.application.PublicCatalogModels.PublicService;
import com.miaupy.catalog.application.PublicCatalogModels.ServicePage;
import com.miaupy.catalog.domain.OfferedServiceRepository;
import com.miaupy.catalog.domain.Product;
import com.miaupy.catalog.domain.ProductRepository;
import com.miaupy.shared.exception.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicCatalogUseCase {
  private final BusinessRepository businesses;
  private final ProductRepository products;
  private final OfferedServiceRepository services;
  private final PublicCatalogCache cache;

  public PublicCatalogUseCase(
      BusinessRepository businesses,
      ProductRepository products,
      OfferedServiceRepository services,
      PublicCatalogCache cache) {
    this.businesses = businesses;
    this.products = products;
    this.services = services;
    this.cache = cache;
  }

  @Transactional(readOnly = true)
  public ProductPage products(String slug, int page, int size) {
    Business business = publicBusiness(slug);
    return cache
        .getProducts(business.tenantId(), page, size)
        .orElseGet(() -> loadProducts(business.tenantId(), page, size));
  }

  @Transactional(readOnly = true)
  public PublicProduct product(String slug, UUID productId) {
    Business business = publicBusiness(slug);
    return products
        .findPublishedByIdAndTenantId(productId, business.tenantId())
        .map(this::productView)
        .orElseThrow(() -> new ResourceNotFoundException("Public product not found"));
  }

  @Transactional(readOnly = true)
  public ServicePage services(String slug, int page, int size) {
    Business business = publicBusiness(slug);
    return cache
        .getServices(business.tenantId(), page, size)
        .orElseGet(() -> loadServices(business.tenantId(), page, size));
  }

  private ProductPage loadProducts(Long tenantId, int page, int size) {
    var result =
        products.findPublishedByTenantId(
            tenantId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    ProductPage value =
        new ProductPage(
            result.getContent().stream().map(this::productView).toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages());
    cache.putProducts(tenantId, value);
    return value;
  }

  private ServicePage loadServices(Long tenantId, int page, int size) {
    var result =
        services.findPublishedByTenantId(
            tenantId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    ServicePage value =
        new ServicePage(
            result.getContent().stream()
                .map(
                    service ->
                        new PublicService(
                            service.id(),
                            service.name(),
                            service.description(),
                            service.durationMinutes(),
                            service.price(),
                            service.requiresApproval()))
                .toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages());
    cache.putServices(tenantId, value);
    return value;
  }

  private Business publicBusiness(String slug) {
    return businesses
        .findPublicBySlug(slug.strip().toLowerCase())
        .orElseThrow(() -> new ResourceNotFoundException("Public store not found"));
  }

  private PublicProduct productView(Product product) {
    return new PublicProduct(
        product.id(),
        product.sku(),
        product.name(),
        product.description(),
        product.price(),
        product.promotionalPrice(),
        product.stockQuantity());
  }
}
