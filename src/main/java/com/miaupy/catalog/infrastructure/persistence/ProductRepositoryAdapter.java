package com.miaupy.catalog.infrastructure.persistence;

import com.miaupy.catalog.domain.Product;
import com.miaupy.catalog.domain.ProductRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

interface SpringDataProductRepository extends JpaRepository<ProductJpaEntity, UUID> {
  Optional<ProductJpaEntity> findByIdAndTenantIdAndActiveTrue(UUID id, Long tenantId);

  Page<ProductJpaEntity> findAllByTenantIdAndActiveTrue(Long tenantId, Pageable pageable);

  Page<ProductJpaEntity> findAllByTenantIdAndActiveTrueAndPublishedTrue(
      Long tenantId, Pageable pageable);

  Optional<ProductJpaEntity> findByIdAndTenantIdAndActiveTrueAndPublishedTrue(
      UUID id, Long tenantId);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  Optional<ProductJpaEntity> findLockedByIdAndTenantId(UUID id, Long tenantId);

  boolean existsBySkuIgnoreCaseAndTenantIdAndIdNotAndActiveTrue(String sku, Long tenantId, UUID id);
}

@Repository
class ProductRepositoryAdapter implements ProductRepository {
  private final SpringDataProductRepository repository;

  ProductRepositoryAdapter(SpringDataProductRepository repository) {
    this.repository = repository;
  }

  public Product save(Product product) {
    return map(repository.save(new ProductJpaEntity(product)));
  }

  public Optional<Product> findByIdAndTenantId(UUID id, Long tenantId) {
    return repository.findByIdAndTenantIdAndActiveTrue(id, tenantId).map(this::map);
  }

  public Page<Product> findAllByTenantId(Long tenantId, Pageable pageable) {
    return repository.findAllByTenantIdAndActiveTrue(tenantId, pageable).map(this::map);
  }

  public Page<Product> findPublishedByTenantId(Long tenantId, Pageable pageable) {
    return repository
        .findAllByTenantIdAndActiveTrueAndPublishedTrue(tenantId, pageable)
        .map(this::map);
  }

  public Optional<Product> findPublishedByIdAndTenantId(UUID id, Long tenantId) {
    return repository.findByIdAndTenantIdAndActiveTrueAndPublishedTrue(id, tenantId).map(this::map);
  }

  public Optional<Product> lockByIdAndTenantId(UUID id, Long tenantId) {
    return repository.findLockedByIdAndTenantId(id, tenantId).map(this::map);
  }

  public boolean existsBySkuAndTenantIdAndDifferentId(String sku, Long tenantId, UUID id) {
    return sku != null
        && repository.existsBySkuIgnoreCaseAndTenantIdAndIdNotAndActiveTrue(sku, tenantId, id);
  }

  private Product map(ProductJpaEntity e) {
    return new Product(
        e.id,
        e.tenantId,
        e.sku,
        e.name,
        e.description,
        e.price,
        e.promotionalPrice,
        e.stockQuantity,
        e.active,
        e.published,
        e.deletedAt,
        e.createdAt,
        e.updatedAt,
        e.version);
  }
}
