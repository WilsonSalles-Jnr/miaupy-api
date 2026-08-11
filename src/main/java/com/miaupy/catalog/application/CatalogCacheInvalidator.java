package com.miaupy.catalog.application;

public interface CatalogCacheInvalidator {
  void invalidateProducts(Long tenantId);

  void invalidateServices(Long tenantId);
}
