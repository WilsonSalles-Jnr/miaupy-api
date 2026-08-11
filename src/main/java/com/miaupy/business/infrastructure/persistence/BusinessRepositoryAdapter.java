package com.miaupy.business.infrastructure.persistence;

import com.miaupy.business.domain.Business;
import com.miaupy.business.domain.BusinessRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class BusinessRepositoryAdapter implements BusinessRepository {

    private final SpringDataBusinessRepository repository;

    BusinessRepositoryAdapter(SpringDataBusinessRepository repository) {
        this.repository = repository;
    }

    @Override
    public Business save(Business business) {
        return toDomain(repository.save(toEntity(business)));
    }

    @Override
    public Optional<Business> findByTenantId(Long tenantId) {
        return repository.findByTenantId(tenantId).map(this::toDomain);
    }

    @Override
    public Optional<Business> findPublicBySlug(String slug) {
        return repository.findBySlugIgnoreCaseAndActiveTrueAndPublicVisibleTrue(slug).map(this::toDomain);
    }

    @Override
    public boolean existsByTenantId(Long tenantId) {
        return repository.existsByTenantId(tenantId);
    }

    @Override
    public boolean existsBySlugAndDifferentTenant(String slug, Long tenantId) {
        return repository.existsBySlugIgnoreCaseAndTenantIdNot(slug, tenantId);
    }

    private BusinessJpaEntity toEntity(Business business) {
        return new BusinessJpaEntity(business.id(), business.tenantId(), business.slug(), business.name(),
                business.tradeName(), business.document(), business.description(), business.phone(), business.email(),
                business.website(), business.active(), business.publicVisible(), business.createdAt(), business.updatedAt(),
                business.version());
    }

    private Business toDomain(BusinessJpaEntity entity) {
        return Business.restore(entity.getId(), entity.getTenantId(), entity.getSlug(), entity.getName(),
                entity.getTradeName(), entity.getDocument(), entity.getDescription(), entity.getPhone(),
                entity.getEmail(), entity.getWebsite(), entity.isActive(), entity.isPublicVisible(),
                entity.getCreatedAt(), entity.getUpdatedAt(), entity.getVersion());
    }
}
