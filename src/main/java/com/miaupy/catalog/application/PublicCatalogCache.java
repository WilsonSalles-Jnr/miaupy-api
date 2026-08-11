package com.miaupy.catalog.application;

import com.miaupy.catalog.application.PublicCatalogModels.ProductPage;
import com.miaupy.catalog.application.PublicCatalogModels.ServicePage;
import java.util.Optional;

public interface PublicCatalogCache {
  Optional<ProductPage> getProducts(Long tenantId, int page, int size);

  void putProducts(Long tenantId, ProductPage value);

  Optional<ServicePage> getServices(Long tenantId, int page, int size);

  void putServices(Long tenantId, ServicePage value);
}
