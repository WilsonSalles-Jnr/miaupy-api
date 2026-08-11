package com.miaupy.business.application;

import com.miaupy.business.domain.Business;
import com.miaupy.business.domain.BusinessRepository;
import com.miaupy.shared.exception.ResourceNotFoundException;
import com.miaupy.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetBusinessProfileUseCase {

    private final TenantContext tenantContext;
    private final BusinessRepository repository;

    public GetBusinessProfileUseCase(TenantContext tenantContext, BusinessRepository repository) {
        this.tenantContext = tenantContext;
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Business execute() {
        Long tenantId = tenantContext.getRequiredTenantId();
        return repository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found"));
    }
}
