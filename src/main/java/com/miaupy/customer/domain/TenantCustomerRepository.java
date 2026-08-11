package com.miaupy.customer.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TenantCustomerRepository {
    TenantCustomer save(TenantCustomer customer);
    Optional<TenantCustomer> findByIdAndTenantId(UUID id,Long tenantId);
    Page<TenantCustomer> findAllByTenantId(Long tenantId,Pageable pageable);
}
