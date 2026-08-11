package com.miaupy.business.domain;

import java.util.Optional;

public interface BusinessRepository {

  Business save(Business business);

  Optional<Business> findByTenantId(Long tenantId);

  Optional<Business> findPublicBySlug(String slug);

  boolean existsByTenantId(Long tenantId);

  boolean existsBySlugAndDifferentTenant(String slug, Long tenantId);
}
