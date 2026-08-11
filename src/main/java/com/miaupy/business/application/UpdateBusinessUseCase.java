package com.miaupy.business.application;

import com.miaupy.business.domain.Business;
import com.miaupy.business.domain.BusinessRepository;
import com.miaupy.shared.exception.ConflictException;
import com.miaupy.shared.exception.ResourceNotFoundException;
import com.miaupy.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateBusinessUseCase {

    private final TenantContext tenantContext;
    private final BusinessRepository repository;

    public UpdateBusinessUseCase(TenantContext tenantContext, BusinessRepository repository) {
        this.tenantContext = tenantContext;
        this.repository = repository;
    }

    @Transactional
    public Business execute(BusinessCommand command) {
        Long tenantId = tenantContext.getRequiredTenantId();
        Business business = repository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found"));
        String slug = CreateBusinessUseCase.normalizeSlug(command.slug());
        if (repository.existsBySlugAndDifferentTenant(slug, tenantId)) {
            throw new ConflictException("The requested public slug is already in use");
        }
        business.update(slug, command.name(), command.tradeName(), command.document(), command.description(),
                command.phone(), command.email(), command.website(), command.publicVisible());
        return repository.save(business);
    }
}
