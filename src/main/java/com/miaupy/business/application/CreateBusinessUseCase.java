package com.miaupy.business.application;

import com.miaupy.business.domain.Business;
import com.miaupy.business.domain.BusinessConfigurationRepository;
import com.miaupy.business.domain.BusinessRepository;
import com.miaupy.business.domain.BusinessSettings;
import com.miaupy.business.domain.AppointmentApprovalMode;
import com.miaupy.shared.exception.ConflictException;
import com.miaupy.shared.tenant.TenantContext;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateBusinessUseCase {

    private final TenantContext tenantContext;
    private final BusinessRepository repository;
    private final BusinessConfigurationRepository configurations;

    public CreateBusinessUseCase(TenantContext tenantContext, BusinessRepository repository,
                                 BusinessConfigurationRepository configurations) {
        this.tenantContext = tenantContext;
        this.repository = repository;
        this.configurations = configurations;
    }

    @Transactional
    public Business execute(BusinessCommand command) {
        Long tenantId = tenantContext.getRequiredTenantId();
        String slug = normalizeSlug(command.slug());
        if (repository.existsByTenantId(tenantId)) {
            throw new ConflictException("The authenticated tenant already has a business profile");
        }
        if (repository.existsBySlugAndDifferentTenant(slug, tenantId)) {
            throw new ConflictException("The requested public slug is already in use");
        }
        Business business = Business.create(tenantId, slug, command.name(), command.tradeName(), command.document(),
                command.description(), command.phone(), command.email(), command.website());
        Business saved = repository.save(business);
        configurations.saveSettings(BusinessSettings.create(tenantId, AppointmentApprovalMode.MANUAL,
                "America/Sao_Paulo", "BRL", false, false));
        return saved;
    }

    static String normalizeSlug(String slug) {
        return slug.strip().toLowerCase(Locale.ROOT);
    }
}
