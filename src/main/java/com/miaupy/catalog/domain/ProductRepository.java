package com.miaupy.catalog.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepository {
  Product save(Product product);

  Optional<Product> findByIdAndTenantId(UUID id, Long tenantId);

  Page<Product> findAllByTenantId(Long tenantId, Pageable pageable);

  Page<Product> findPublishedByTenantId(Long tenantId, Pageable pageable);

  Optional<Product> findPublishedByIdAndTenantId(UUID id, Long tenantId);

  boolean existsBySkuAndTenantIdAndDifferentId(String sku, Long tenantId, UUID id);
}
