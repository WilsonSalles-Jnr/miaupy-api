package com.miaupy.catalog.application;

import com.miaupy.catalog.domain.Product;
import com.miaupy.catalog.domain.ProductRepository;
import com.miaupy.integration.outbox.OutboxWriter;
import com.miaupy.shared.exception.ConflictException;
import com.miaupy.shared.exception.ResourceNotFoundException;
import com.miaupy.shared.tenant.TenantContext;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductUseCase {
  private final TenantContext tenantContext;
  private final ProductRepository repository;
  private final OutboxWriter outbox;
  private final CatalogCacheInvalidator cache;

  public ProductUseCase(
      TenantContext tenantContext,
      ProductRepository repository,
      OutboxWriter outbox,
      CatalogCacheInvalidator cache) {
    this.tenantContext = tenantContext;
    this.repository = repository;
    this.outbox = outbox;
    this.cache = cache;
  }

  @Transactional
  public Product create(Command command) {
    Long tenantId = tenantContext.getRequiredTenantId();
    Product product =
        Product.create(
            tenantId,
            command.sku(),
            command.name(),
            command.description(),
            command.price(),
            command.promotionalPrice(),
            command.stockQuantity());
    ensureSkuAvailable(product, tenantId);
    Product saved = repository.save(product);
    append(saved, "product.created");
    return saved;
  }

  @Transactional(readOnly = true)
  public Product get(UUID id) {
    return required(id, tenantContext.getRequiredTenantId());
  }

  @Transactional(readOnly = true)
  public Page<Product> list(int page, int size) {
    return repository.findAllByTenantId(
        tenantContext.getRequiredTenantId(),
        PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")));
  }

  @Transactional
  public Product update(UUID id, Command command) {
    Long tenantId = tenantContext.getRequiredTenantId();
    Product updated =
        required(id, tenantId)
            .update(
                command.sku(),
                command.name(),
                command.description(),
                command.price(),
                command.promotionalPrice(),
                command.stockQuantity());
    ensureSkuAvailable(updated, tenantId);
    Product saved = repository.save(updated);
    append(saved, "product.updated");
    cache.invalidateProducts(tenantId);
    return saved;
  }

  @Transactional
  public Product publish(UUID id) {
    return changePublication(id, true);
  }

  @Transactional
  public Product unpublish(UUID id) {
    return changePublication(id, false);
  }

  @Transactional
  public void delete(UUID id) {
    Long tenantId = tenantContext.getRequiredTenantId();
    Product saved = repository.save(required(id, tenantId).deactivate());
    append(saved, "product.updated");
    cache.invalidateProducts(tenantId);
  }

  private Product changePublication(UUID id, boolean publish) {
    Long tenantId = tenantContext.getRequiredTenantId();
    Product current = required(id, tenantId);
    Product saved = repository.save(publish ? current.publish() : current.unpublish());
    append(saved, publish ? "product.published" : "product.unpublished");
    cache.invalidateProducts(tenantId);
    return saved;
  }

  private Product required(UUID id, Long tenantId) {
    return repository
        .findByIdAndTenantId(id, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
  }

  private void ensureSkuAvailable(Product product, Long tenantId) {
    if (repository.existsBySkuAndTenantIdAndDifferentId(product.sku(), tenantId, product.id())) {
      throw new ConflictException("Product SKU is already in use by this tenant");
    }
  }

  private void append(Product product, String eventType) {
    outbox.append(
        "Product",
        product.id(),
        eventType,
        product.tenantId(),
        Map.of(
            "productId", product.id(),
            "name", product.name(),
            "active", product.active(),
            "published", product.published()));
  }

  public record Command(
      String sku,
      String name,
      String description,
      BigDecimal price,
      BigDecimal promotionalPrice,
      int stockQuantity) {}
}
